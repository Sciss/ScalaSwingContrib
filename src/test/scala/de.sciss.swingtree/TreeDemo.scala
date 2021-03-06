package de.sciss.swingtree

import java.net.URL

import de.sciss.swingtree.Tree.{Editor, Renderer}
import de.sciss.swingtree.event.TreeNodeSelected
import javax.swing.{Icon => JIcon}

import scala.collection.{Seq => CSeq}
import scala.collection.mutable
import scala.swing.Swing.{Icon, pair2Dimension}
import scala.swing.{Action, BorderPanel, Button, Component, Dimension, Frame, GridPanel, Label, MainFrame, ScrollPane, SimpleSwingApplication, TabbedPane}
import scala.xml.XML

object TreeDemo extends SimpleSwingApplication {
  import java.io._

  import ExampleData._
      
  // Use case 1: Show an XML document
  lazy val xmlTree: Tree[xml.Node] = new Tree[xml.Node] {
    model = TreeModel(xmlDoc)(_.child.filterNot(_.text.trim.isEmpty))

    renderer = Renderer { n =>
      if (n.label startsWith "#") n.text.trim
      else n.label
    }
        
    expandAll()
  }
  
  
  // Use case 2: Show the filesystem with filter
  lazy val fileSystemTree: Tree[File] = new Tree[File] {
    model = TreeModel(new File(".")) { f =>
      if (f.isDirectory) f.listFiles.toSeq 
      else Seq()
    }
    
    renderer = Renderer.labeled { f =>
      val icon = if (f.isDirectory) folderIcon else fileIcon
      (icon, f.getName)
    }

    expandRow(0)
  }
  
  // Use case 3: Object graph containing diverse elements, reacting to clicks
  lazy val objectGraphTree: Tree[Any] = new Tree[Any] {
    model = TreeModel[Any](orders: _*) {
      case o @ Order(_, cust, prod, qty) => Seq(cust, prod, "Qty" -> qty, "Cost" -> ("$" + o.price))
      case Product(id, name, price) => Seq("ID" -> id, "Name" -> name, "Price" -> ("$" + price))
      case Customer(id, _, first, last) => Seq("ID" -> id, "First name" -> first, "Last name" -> last)
      case _ => Seq.empty
    } 

    renderer = Renderer({
      case Order  (id, _, _, 1)             => s"Order #$id"
      case Order  (id, _, _, qty)           => s"Order #$id x $qty"
      case Product(id, _, _)                => s"Product $id"
      case Customer(_, title, first, last)  => s"$title $first $last"
      case (field, value)                   => s"$field: $value"
      case x                                => x.toString
    })

    expandAll()
  }
  
  // Use case 4: Infinitely deep structure
  lazy val infiniteTree: Tree[Int] = new Tree(TreeModel(1000) {n => 1 to n filter (n % _ == 0)}) {
    expandRow(0)
  }

  val externalTreeStatusBar: Label = new Label {
    preferredSize = (100,12)
  }

  val internalTreeStatusBar: Label = new Label {
    preferredSize = (100,12)
  }
  
  // Use case 5: Mutable external tree model
  val mutableExternalTree: Tree[PretendFile] = new Tree[PretendFile] {

    model = ExternalTreeModel(pretendFileSystem)(_.children).makeUpdatableWith {
      (pathOfFile, updatedFile) =>       
          val succeeded = pathOfFile.last.rename(updatedFile.name)
          externalTreeStatusBar.text = "Updating file " + (if (succeeded) "succeeded" else "failed")
          pathOfFile.last
          
    }.makeInsertableWith {
      (parentPath, fileToInsert, index) => 
        val parentDir = parentPath.last
        if (parentDir.children contains fileToInsert) false
        else parentDir.insertChild(fileToInsert, index)
      
    }.makeRemovableWith { pathToRemove =>
      if (pathToRemove.length >= 2) pathToRemove.last.delete()
      else false
    }

    listenTo(selection)
    reactions += {
      case TreeNodeSelected(node) => externalTreeStatusBar.text = "Selected: " + node
    }
    
    renderer = Renderer.labeled { f =>
      val icon = if (f.isDirectory) folderIcon 
                 else fileIcon
      (icon, f.name)
    }
    editor = Editor((_: PretendFile).name, PretendFile(_: String))
    expandRow(0)
  }

  // Use case 6: Mutable internal tree model
  val mutableInternalTree: Tree[PretendFile] = new Tree[PretendFile] {

    model = InternalTreeModel(pretendFileSystem)(_.children)

    listenTo(selection)
    reactions += {
      case TreeNodeSelected(node) => internalTreeStatusBar.text = "Selected: " + node
    }
    
    renderer  = mutableExternalTree.renderer
    editor    = mutableExternalTree.editor
    expandRow(0)
  }
  
  
  class ButtonPanel(pretendFileTree: Tree[PretendFile], setStatus: String => Unit) extends GridPanel(10,1) {
    
    val updateButton = new Button(Action("Directly update") {
      val pathToRename = pretendFileTree.selection.paths.leadSelection
      for (path <- pathToRename) {
        val oldName = path.last.name
        pretendFileTree.model.update(path, PretendFile("directly-updated-file"))
        setStatus("Updated " + oldName)
      }
    })
    
    val editButton = new Button(Action("Edit") {
      val pathToEdit = pretendFileTree.selection.paths.leadSelection
      for (path <- pathToEdit) {
        pretendFileTree.startEditingAtPath(path)
        setStatus("Editing... ")
      }
    })
    
    val insertButton = new Button(Action("Insert under") {
      val pathToInsertUnder = pretendFileTree.selection.paths.leadSelection
      for (path <- pathToInsertUnder) {
        val succeeded = pretendFileTree.model.insertUnder(path, PretendFile("new-under-" + path.last.name), 0)
        setStatus("Inserting " + (if (succeeded) "succeeded" else "failed"))
      }
    })
    
    val insertBeforeButton = new Button(Action("Insert before") {
      val pathToInsertBefore = pretendFileTree.selection.paths.leadSelection
      for (path <- pathToInsertBefore) {
        val succeeded = pretendFileTree.model.insertBefore(path, PretendFile("new-before-" + path.last.name))
        setStatus("Inserting " + (if (succeeded) "succeeded" else "failed"))
      }
    })
    
    val insertAfterButton = new Button(Action("Insert after") {
      val pathToInsertAfter = pretendFileTree.selection.paths.leadSelection
      for (path <- pathToInsertAfter) {
        val succeeded = pretendFileTree.model.insertAfter(path, PretendFile("new-after-" + path.last.name))
        setStatus("Inserting " + (if (succeeded) "succeeded" else "failed"))
      }
    })
    
    val removeButton = new Button(Action("Remove") {
      val pathToRemove = pretendFileTree.selection.paths.leadSelection
      for (path <- pathToRemove) {
        val succeeded = pretendFileTree.model remove path
        setStatus("Remove " + (if (succeeded) "succeeded" else "failed"))
      }
    })
    
    contents += editButton
    contents += updateButton
    contents += insertButton
    contents += insertBeforeButton
    contents += insertAfterButton
    contents += removeButton
  }

  // Other setup stuff

  lazy val top: Frame = new MainFrame {
    title = "Scala Swing Tree Demo"
  
    contents = new TabbedPane {
      import BorderPanel.Position._
      import TabbedPane.Page
      
      def southCenterAndEast(north: Component, center: Component, east: Component): BorderPanel = new BorderPanel {
        layout(north  ) = South
        layout(center ) = Center
        layout(east   ) = East
      }
      
      pages += new Page("1: XML file"             , new ScrollPane(xmlTree))
      pages += new Page("2: File system"          , new ScrollPane(fileSystemTree))
      pages += new Page("3: Diverse object graph" , new ScrollPane(objectGraphTree))
      pages += new Page("4: Infinite structure"   , new ScrollPane(infiniteTree))
      pages += new Page("5: Mutable external model", southCenterAndEast(
        externalTreeStatusBar, 
        new ScrollPane(mutableExternalTree),
        new ButtonPanel(mutableExternalTree, externalTreeStatusBar.text_=)))
      
      pages += new Page("6: Mutable internal model", southCenterAndEast(
        internalTreeStatusBar, 
        new ScrollPane(mutableInternalTree),
        new ButtonPanel(mutableInternalTree, internalTreeStatusBar.text_=)))
    }
    
    size = (1024, 768): Dimension
  }

  object ExampleData {
    val rsrc = "test"
    
    // File system icons
    def getIconUrl(path: String): URL = resourceFromClassloader(path).ensuring(_ != null, s"Couldn't find icon $path")

    val fileIcon  : JIcon = Icon(getIconUrl(s"$rsrc/images/file.png"))
    val folderIcon: JIcon = Icon(getIconUrl(s"$rsrc/images/folder.png"))
    
    // Contrived class hierarchy
    case class Customer(id: Int, title: String, firstName: String, lastName: String)

    case class Product(id: String, name: String, price: Double)

    case class Order(id: Int, customer: Customer, product: Product, quantity: Int) {
      def price: Double = product.price * quantity
    }

    // Contrived example data
    val bob         = Customer(1, "Mr", "Bob"   , "Baxter"      )
    val fred        = Customer(2, "Dr", "Fred"  , "Finkelstein" )
    val susan       = Customer(3, "Ms", "Susan" , "Smithers"    )

    val powerSaw    = Product("X-123", "Power Saw"    ,  99.95)
    val nailGun     = Product("Y-456", "Nail gun"     , 299.95)
    val boxOfNails  = Product("Z-789", "Box of nails" ,  23.50)

    val orders: List[Order] = List(
      Order(1, fred , powerSaw  ,  1),
      Order(2, fred , boxOfNails,  3),
      Order(3, bob  , boxOfNails, 44),
      Order(4, susan, nailGun   ,  1)
    )
      
    lazy val xmlDoc: xml.Node =
      try {
        XML.load(resourceFromClassloader(s"$rsrc/sample.xml"))
      } catch {
        case _: IOException =>
          <error> Error reading XML file. </error>
      }

    // Pretend file system, so we can safely add/edit/delete stuff
    case class PretendFile(private var nameVar: String, private val childFiles: PretendFile*) {
      var parent: Option[PretendFile] = None

      childFiles.foreach(_.parent = Some(this))

      private val childBuffer = mutable.ListBuffer(childFiles: _*)
      
      override def toString: String = name

      def name: String = nameVar

      def rename(str: String): Boolean =
        !siblingExists(str) && {
          nameVar = str; true
        }

      def insertChild(child: PretendFile, index: Int): Boolean =
        (isDirectory && !childExists(child.name)) && {
          child.parent = Some(this)
          childBuffer.insert(index, child)
          true 
        }

      def delete(): Boolean = parent.exists(_.removeChild(this))

      def removeChild(child: PretendFile): Boolean =
        children.contains(child) && {
          childBuffer -= child; true
        }

      def siblingExists(siblingName: String): Boolean = parent.exists(_.childExists(siblingName))

      def childExists(childName: String): Boolean = children.exists(_.name == childName)

      def children: CSeq[PretendFile] = childBuffer

      def isDirectory: Boolean = children.nonEmpty
    }
    
    val pretendFileSystem = PretendFile("~", 
        PretendFile("lib", 
            PretendFile("coolstuff-1.1.jar"),
            PretendFile("coolstuff-1.2.jar"),
            PretendFile("robots-0.2.5.jar")), 
        PretendFile("bin", 
            PretendFile("cleanup"), 
            PretendFile("morestuff"), 
            PretendFile("dostuff")), 
        PretendFile("tmp", 
            PretendFile("log", 
                PretendFile("1.log"),
                PretendFile("2.log"),
                PretendFile("3.log"),
                PretendFile("4.log")), 
            PretendFile("readme.txt"), 
            PretendFile("foo.bar"), 
            PretendFile("bar.foo"), 
            PretendFile("dingus")), 
        PretendFile("something.moo"))
  }
}



