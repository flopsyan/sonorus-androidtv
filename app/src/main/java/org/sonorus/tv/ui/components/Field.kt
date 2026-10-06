package org.sonorus.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import org.sonorus.tv.ui.theme.SonorusTheme

/** A labelled one-line text field with the same amber focus frame as the cards. */
@Composable
fun TvTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val colors = SonorusTheme.colors
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Column(modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (focused) colors.text else colors.textDim)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(if (focused) colors.surface3 else colors.surface2, shape)
                        .border(if (focused) 3.dp else 1.dp, if (focused) colors.accent else colors.line, shape)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.textFaint)
                    }
                    inner()
                }
            },
        )
    }
}
