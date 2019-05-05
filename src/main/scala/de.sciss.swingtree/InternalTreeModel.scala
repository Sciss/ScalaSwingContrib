package de.sciss.swingtree

import de.sciss.swingtree.InternalTreeModel.{PeerModel, PeerNode}
import de.sciss.swingtree.Tree.Path
import de.sciss.swingtree.TreeModel.hiddenRoot
import javax.swing.{tree => jst}

import scala.collection.JavaConverters.enumerationAsScalaIteratorConverter
import scala.collection.{Seq => CSeq}

object InternalTreeModel {
  
  def empty[A] = new InternalTreeModel[A](new PeerModel(new jst.DefaultMutableTreeNode(hiddenRoot)))
  
  def apply[A](roots: A*)(children: A => CSeq[A]): InternalTreeModel[A] = {
    def createNode(a: A): PeerNode = {
      val node = new PeerNode(a)
      children(a).iterator.map(createNode).foreach(node.add)
      node
    }
    
    val rootNode = new PeerNode(hiddenRoot)
    roots.iterator.map(createNode).foreach(rootNode.add)
    
    new InternalTreeModel(new PeerModel(rootNode))
  }
  
  private[swingtree] type PeerModel = jst.DefaultTreeModel
  private[swingtree] type PeerNode  = jst.DefaultMutableTreeNode
}


class InternalTreeModel[A] private (val peer: PeerModel) extends TreeModel[A] { 
  self =>
    
  def this() = this(new PeerModel(new PeerNode(hiddenRoot)))
    
  def pathToTreePath(path: Path[A]): jst.TreePath = {
    
    val nodePath = path.foldLeft(List(rootPeerNode)) { (pathList, a) => 
      val childNodes = getNodeChildren(pathList.head)
      val node = childNodes.find(_.getUserObject == a) getOrElse sys.error("Couldn't find internal node for " + a)
      node :: pathList
    }.reverse

    val array = nodePath.toArray[AnyRef]
    new jst.TreePath(array)
  }

  def treePathToPath(tp: jst.TreePath): Path[A] = {
    if (tp == null) null 
    else {
      val b = Path.newBuilder[A]
      val p = tp.getPath
      b.sizeHint(p.length)
      var i = 1
      while (i < p.length) {
        val n = unpackNode(p(i))
        b += n
        i += 1
      }
      b.result()
    }
  } 
  
  private def rootPeerNode = peer.getRoot.asInstanceOf[PeerNode]

  def roots: Seq[A] = getNodeChildren(rootPeerNode) map unpackNode
  
  def update(path: Path[A], newValue: A): Unit =
    peer.valueForPathChanged(pathToTreePath(path), newValue)

  private def getPeerNodeAt(path: Path[A]): PeerNode = {
    pathToTreePath(path).getLastPathComponent.asInstanceOf[PeerNode]
  }
  
  def insertUnder(parentPath: Path[A], newValue: A, index: Int): Boolean = {
    peer.insertNodeInto(new PeerNode(newValue), getPeerNodeAt(parentPath), index)
    true
  }
  
  def remove(pathToRemove: Path[A]): Boolean = {
    peer.removeNodeFromParent(getPeerNodeAt(pathToRemove))
    true
  }
  
  def map[B](f: A => B): InternalTreeModel[B] = new InternalTreeModel[B] {
    override val peer: PeerModel = copyFromModel(self, f)
  }

  protected[swingtree] def copyFromModel[B](otherModel: TreeModel[B], f: B => A): jst.DefaultTreeModel = {
    def copyNodeAt(bPath: Path[B]): PeerNode = {
      val copiedNode     = new PeerNode(f(bPath.last))
      val otherChildren  = otherModel.getChildrenOf(bPath)
      val copiedChildren = otherChildren map { b => copyNodeAt(bPath :+ b) }
      copiedChildren foreach copiedNode.add
      copiedNode
    }
    
    val rootNode = new PeerNode(hiddenRoot)
    val children = otherModel.roots map { b => copyNodeAt(Path(b)) }
    children foreach rootNode.add
    new jst.DefaultTreeModel(rootNode)
  }
  
  private def getNodeChildren(node: PeerNode): Seq[PeerNode] = {
    val sq: Seq[Any] = node.children.asScala.toSeq
    sq.asInstanceOf[Seq[PeerNode]]
  }
  
  def getChildrenOf(parentPath: Path[A]): Seq[A] = {
    val lastNode = pathToTreePath(parentPath).getLastPathComponent.asInstanceOf[PeerNode]
    getNodeChildren(lastNode) map unpackNode
  }

  def filter(p: A => Boolean): InternalTreeModel[A] = {
    def filterChildren(node: PeerNode): PeerNode = {
      val newNode = new PeerNode(node.getUserObject)
      val okChildren = getNodeChildren(node) filter { n => p(unpackNode(n)) }
      okChildren map filterChildren foreach newNode.add
      newNode
    }
    new InternalTreeModel(new PeerModel(filterChildren(rootPeerNode)))
  }
    
  def toInternalModel: InternalTreeModel[A] = this
  
  def isExternalModel = false
  
  override def unpackNode(node: Any): A = node.asInstanceOf[PeerNode].getUserObject.asInstanceOf[A]
}
