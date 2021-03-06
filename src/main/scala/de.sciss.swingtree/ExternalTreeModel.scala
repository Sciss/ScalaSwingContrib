package de.sciss.swingtree

import de.sciss.swingtree.Tree.Path
import javax.swing.{event => jse, tree => jst}

import scala.collection.{Seq => CSeq}
import scala.collection.immutable.{Seq => ISeq}

object ExternalTreeModel {
  def empty[A]: ExternalTreeModel[A] = new ExternalTreeModel[A](Seq.empty, _ => Seq.empty)
  def apply[A](roots: A*)(children: A => CSeq[A]): ExternalTreeModel[A] =
    new ExternalTreeModel[A](roots, children)
}

/** Represents tree data as a sequence of root nodes, and a function that can retrieve child nodes. */
class ExternalTreeModel[A](rootItems: CSeq[A], children: A => CSeq[A]) extends TreeModel[A] {
  self =>
  
  import TreeModel._
  
  private var rootsVar = rootItems.toList

  def roots: ISeq[A] = rootsVar
  
  def getChildrenOf(parentPath: Path[A]): CSeq[A] =
    if (parentPath.isEmpty) roots
    else children(parentPath.last)
  
  def filter(p: A => Boolean): ExternalTreeModel[A] =
    new ExternalTreeModel[A](roots.filter(p), a => children(a).filter(p))

  def toInternalModel: InternalTreeModel[A] = InternalTreeModel(roots: _*)(children)
  
  def isExternalModel = true
  
  def map[B](f: A => B): InternalTreeModel[B] = toInternalModel map f
  
  def pathToTreePath(path: Path[A]): jst.TreePath = {
    val array = (hiddenRoot +: path).map(_.asInstanceOf[AnyRef]).toArray
    new jst.TreePath(array)
  }
  
  def treePathToPath(tp: jst.TreePath): Path[A] =
    if (tp == null) null 
    else {
      val p = tp.getPath
      val b = Path.newBuilder[A]
      b.sizeHint(p.length)
      var i = 1
      while (i < p.length) {
        val n = p(i).asInstanceOf[A]
        b += n
        i += 1
      }
      b.result()
    }

  /** A function to update a value in the model, at a given path.  By default this will throw an exception; to
    * make a TreeModel updatable, call makeUpdatable() to provide a new TreeModel with the specified update method.
    */
  protected[swingtree] val updateFunc: (Path[A], A) => A = {
    (_,_) => throw new UnsupportedOperationException("Update is not supported on this tree")
  }

  /** A function to insert a value in the model at a given path, returning whether the operation succeeded.
    * By default this will throw an exception; to allow insertion on a TreeModel,
    * call insertableWith() to provide a new TreeModel with the specified insert method.
    */
  protected[swingtree] val insertFunc: (Path[A], A, Int) => Boolean = {
    (_,_,_) => throw new UnsupportedOperationException("Insert is not supported on this tree")
  }

  /** A function to remove a item in the model at a given path, returning whether the operation succeeded.
    * By default this will throw an exception; to allow removal from a TreeModel,
    * call removableWith() to provide a new TreeModel with the specified remove method.
    */
  protected[swingtree] val removeFunc: Path[A] => Boolean = {
    _ => throw new UnsupportedOperationException("Removal is not supported on this tree")
  }

  /** Returns a new VirtualTreeModel that knows how to modify the underlying representation,
    * using the given function to replace one value with another.
    * <p>
    * Calling update() on a model returned from makeUpdatable() will perform the update.
    */
  def makeUpdatableWith(effectfulUpdate: (Path[A], A) => A): ExternalTreeModel[A] =
    new ExternalTreeModel(roots, children) {
      override val updateFunc: (Path[A], A) => A            = effectfulUpdate
      override val insertFunc: (Path[A], A, Int) => Boolean = self.insertFunc
      override val removeFunc:  Path[A]          => Boolean = self.removeFunc
      this.peer copyListenersFrom self.peer
    }

  def makeInsertableWith(effectfulInsert: (Path[A], A, Int) => Boolean): ExternalTreeModel[A] =
    new ExternalTreeModel(roots, children) {
      override val updateFunc: (Path[A], A) => A            = self.updateFunc
      override val insertFunc: (Path[A], A, Int) => Boolean = effectfulInsert
      override val removeFunc:  Path[A]          => Boolean = self.removeFunc
      this.peer copyListenersFrom self.peer
    }
  
  def makeRemovableWith(effectfulRemove: Path[A] => Boolean): ExternalTreeModel[A] =
    new ExternalTreeModel(roots, children) {
      override val updateFunc: (Path[A], A) => A            = self.updateFunc
      override val insertFunc: (Path[A], A, Int) => Boolean = self.insertFunc
      override val removeFunc:  Path[A]          => Boolean = effectfulRemove
      this.peer copyListenersFrom self.peer
    }

  /** Replaces one value with another, mutating the underlying structure.
    * If a way to modify the external tree structure has not been provided with makeUpdatableWith(), then
    * an exception will be thrown.
    */
  def update(path: Path[A], newValue: A): Unit = {
    if (path.isEmpty) throw new IllegalArgumentException("Cannot update an empty path")
    
    val existing = path.last
    val result = updateFunc(path, newValue)

    val replacingWithDifferentReference = existing.isInstanceOf[AnyRef] && 
                                         (existing.asInstanceOf[AnyRef] ne result.asInstanceOf[AnyRef])
       
    
    // If the result is actually replacing the node with a different reference object, then 
    // fire "tree structure changed".
    if (replacingWithDifferentReference) {
      if (path.size == 1) {
        rootsVar = rootsVar.updated(roots indexOf newValue, newValue)
      }
      
      peer.fireTreeStructureChanged(pathToTreePath(path.init), result)
    }
    // If the result is a value type or is a modification of the same node reference, then
    // just fire "nodes changed".
    else {
      peer.fireNodesChanged(pathToTreePath(path.init), result)
    }
  }
  
  def insertUnder(parentPath: Path[A], newValue: A, index: Int): Boolean = {
    val succeeded = if (parentPath.nonEmpty) {
      insertFunc(parentPath, newValue, index)
    }
    else { 
      val (before, after) = rootsVar splitAt index
      rootsVar = before ::: newValue :: after
      true 
    }
                                                     
    if (succeeded) {
      val actualIndex = siblingsUnder(parentPath) indexOf newValue
      if (actualIndex == -1) return false
        
      peer.fireNodesInserted(pathToTreePath(parentPath), newValue, actualIndex)
    }
    succeeded
  }
  
  def remove(pathToRemove: Path[A]): Boolean = {
    if (pathToRemove.isEmpty) return false
    
    val parentPath = pathToRemove.init
    val index = siblingsUnder(parentPath) indexOf pathToRemove.last
    if (index == -1) return false
      
    val succeeded = if (pathToRemove.size == 1) {
      rootsVar = rootsVar.filterNot(pathToRemove.last == _)
      true
    }
    else {
      removeFunc(pathToRemove)
    }
    
    if (succeeded) {

      peer.fireNodesRemoved(pathToTreePath(parentPath), pathToRemove.last, index)
    }
    succeeded
  }

  
  class ExternalTreeModelPeer extends jst.TreeModel {
    private var treeModelListenerList = List.empty[jse.TreeModelListener]

    def getChildrenOf(parent: Any): CSeq[A] = parent match {
      case `hiddenRoot` => roots
      case a            => children(a.asInstanceOf[A])
    }
    
    def getChild(parent: Any, index: Int): AnyRef = {
      val ch = getChildrenOf(parent)
      if (index >= 0 && index < ch.size) 
        ch(index).asInstanceOf[AnyRef] 
      else 
        throw new IndexOutOfBoundsException("No child of \"" + parent + "\" found at index " + index)
    }

    def getChildCount  (parent: Any)            : Int = getChildrenOf(parent).size
    def getIndexOfChild(parent: Any, child: Any): Int = getChildrenOf(parent) indexOf child

    def getRoot: AnyRef = hiddenRoot
    def isLeaf(node: Any): Boolean = getChildrenOf(node).isEmpty

    private[swingtree] def copyListenersFrom(otherPeer: ExternalTreeModel[A]#ExternalTreeModelPeer): Unit =
      otherPeer.treeModelListeners.foreach(addTreeModelListener)

    def treeModelListeners: ISeq[jse.TreeModelListener] = treeModelListenerList

    def addTreeModelListener(tml: jse.TreeModelListener): Unit =
      treeModelListenerList ::= tml

    def removeTreeModelListener(tml: jse.TreeModelListener): Unit  =
      treeModelListenerList = treeModelListenerList.filterNot(_ == tml)

    def valueForPathChanged(path: jst.TreePath, newValue: Any): Unit =
      update(treePathToPath(path), newValue.asInstanceOf[A])

    private def createEvent(parentPath: jst.TreePath, newValue: Any): jse.TreeModelEvent = {
      val index = getChildrenOf(parentPath.getPath.last) indexOf newValue
      createEventWithIndex(parentPath, newValue, index)
    }
  
    private def createEventWithIndex(parentPath: jst.TreePath, newValue: Any, index: Int): jse.TreeModelEvent = {
      new jse.TreeModelEvent(this, parentPath, Array(index), Array(newValue.asInstanceOf[AnyRef]))
    }
    
    def fireTreeStructureChanged(parentPath: jst.TreePath, newValue: Any): Unit =
      treeModelListenerList foreach { _ treeStructureChanged createEvent(parentPath, newValue) }

    def fireNodesChanged(parentPath: jst.TreePath, newValue: Any): Unit =
      treeModelListenerList foreach { _ treeNodesChanged createEvent(parentPath, newValue) }

    def fireNodesInserted(parentPath: jst.TreePath, newValue: Any, index: Int): Unit = {
      def createEvent = createEventWithIndex(parentPath, newValue, index)
      treeModelListenerList foreach { _ treeNodesInserted createEvent }
    }
    
    def fireNodesRemoved(parentPath: jst.TreePath, removedValue: Any, index: Int): Unit = {
      def createEvent = createEventWithIndex(parentPath, removedValue, index)
      treeModelListenerList foreach { _ treeNodesRemoved createEvent }
    }
  }

  /** Underlying tree model that exposes the tree structure to Java Swing.
    *
    * This implementation of javax.swing.tree.TreeModel takes advantage of its abstract nature, so that it respects
    * the tree shape of the underlying structure provided by the user.
    */
  lazy val peer = new ExternalTreeModelPeer
}