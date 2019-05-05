package de.sciss.swingtree

final case class CellInfo(isSelected: Boolean = false, isExpanded: Boolean = false,
                          isLeaf: Boolean = false, row: Int = 0, hasFocus: Boolean = false)