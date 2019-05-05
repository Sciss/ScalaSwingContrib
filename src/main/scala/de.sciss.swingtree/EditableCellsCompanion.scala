package de.sciss.swingtree

import de.sciss.swingtree
import de.sciss.swingtree.event.{CellEditingCancelled, CellEditingStopped}
import javax.swing.{event => jse}
import javax.{swing => js}

import scala.language.higherKinds
import scala.swing.{Component, Publisher}

/** Describes the structure of a component's companion object where pluggable cell editors must be supported.
  * @author Ken Scambler
  */
trait EditableCellsCompanion {
  type Editor[A] <: CellEditor[A]
  protected type Owner <: Component with CellView[_]
  
  val Editor: CellEditorCompanion
  

  trait CellEditorCompanion {
    type Peer <: js.CellEditor
    val emptyCellInfo: CellInfo
    def wrap[A](e: Peer): Editor[A]
    def apply[A, B: Editor](toB: A => B, toA: B => A): Editor[A]
  }
  
  trait CellEditor[A] extends Publisher with swingtree.CellEditor[A] {
    val companion: CellEditorCompanion

    def peer: companion.Peer

    protected def fireCellEditingCancelled(): Unit = publish(CellEditingCancelled[A](this))
    protected def fireCellEditingStopped  (): Unit = publish(CellEditingStopped  [A](this))

    protected def listenToPeer(p: js.CellEditor): Unit =
      p.addCellEditorListener(new jse.CellEditorListener {
        override def editingCanceled(e: jse.ChangeEvent): Unit = fireCellEditingCancelled()
        override def editingStopped (e: jse.ChangeEvent): Unit = fireCellEditingStopped  ()
      })

    abstract class EditorPeer extends js.AbstractCellEditor {
      override def getCellEditorValue: AnyRef = value.asInstanceOf[AnyRef]
      listenToPeer(this)
    }

    def componentFor(owner: Owner, value: A, cellInfo: CellInfo): Component
    
    def cellEditable       : Boolean  = peer.isCellEditable(null)
    def shouldSelectCell   : Boolean  = peer.shouldSelectCell(null)
    def cancelCellEditing(): Unit     = peer.cancelCellEditing()
    def stopCellEditing()  : Boolean  = peer.stopCellEditing
  }
}