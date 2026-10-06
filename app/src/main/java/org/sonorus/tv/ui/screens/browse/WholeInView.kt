package org.sonorus.tv.ui.screens.browse

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.relocation.BringIntoViewModifierNode
import androidx.compose.ui.relocation.bringIntoView

/** When anything inside asks to be scrolled into view, the scrolling parent shows all of this instead. */
fun Modifier.bringWholeIntoView(): Modifier = this then WholeInViewElement

private data object WholeInViewElement : ModifierNodeElement<WholeInViewNode>() {
    override fun create() = WholeInViewNode()
    override fun update(node: WholeInViewNode) {}
}

private class WholeInViewNode : Modifier.Node(), BringIntoViewModifierNode {
    override suspend fun bringIntoView(childCoordinates: LayoutCoordinates, boundsProvider: () -> Rect?) {
        bringIntoView()
    }
}
