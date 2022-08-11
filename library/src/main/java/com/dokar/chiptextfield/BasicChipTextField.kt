package com.dokar.chiptextfield

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dokar.chiptextfield.util.filterNewLine
import com.google.accompanist.flowlayout.FlowCrossAxisAlignment
import com.google.accompanist.flowlayout.FlowRow

/**
 * A text field can display chips, press enter to create a new chip.
 *
 * @param state Use [rememberChipTextFieldState] to create new state.
 * @param modifier Modifier for chip text field.
 * @param onSubmit Called after pressing enter key.
 * @param enabled Enabled state, if false, user will not able to edit and select.
 * @param readOnly If true, edit will be disabled, but user can still select text.
 * @param readOnlyChips If true, chips are no more editable, but the text field can still be edited
 * if [readOnly] is not true.
 * @param isError Error state, it is used to change cursor color.
 * @param keyboardOptions See [BasicTextField] for the details.
 * @param textStyle Text style, also apply to text in chips.
 * @param chipStyle Chip style, include shape, text color, background color, etc. See [ChipStyle].
 * @param chipVerticalSpacing Vertical spacing between chips.
 * @param chipHorizontalSpacing Horizontal spacing between chips.
 * @param chipLeadingIcon Leading chip icon, nothing will be displayed by default.
 * @param chipTrailingIcon Trailing chip icon, by default, a [CloseButton] will be displayed.
 * @param onChipClick Chip click action.
 * @param onChipLongClick Chip long click action.
 * @param colors Text colors. [TextFieldDefaults.textFieldColors] is default colors.
 * @param decorationBox The decoration box to wrap around text field.
 *
 * @see BasicTextField
 */
@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun <T : Chip> BasicChipTextField(
    state: ChipTextFieldState<T>,
    modifier: Modifier = Modifier,
    onSubmit: (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    readOnlyChips: Boolean = readOnly,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = LocalTextStyle.current,
    chipStyle: ChipStyle = ChipTextFieldDefaults.chipStyle(),
    chipVerticalSpacing: Dp = 4.dp,
    chipHorizontalSpacing: Dp = 4.dp,
    chipLeadingIcon: @Composable (chip: T) -> Unit = {},
    chipTrailingIcon: @Composable (chip: T) -> Unit = { CloseButton(state, it) },
    onChipClick: ((chip: T) -> Unit)? = null,
    onChipLongClick: ((chip: T) -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: TextFieldColors = TextFieldDefaults.textFieldColors(),
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() },
) {
    val textFieldFocusRequester = remember { FocusRequester() }

    val editable = enabled && !readOnly

    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(state, state.disposed) {
        if (state.disposed) {
            state.chips = state.defaultChips
            state.disposed = false
        }
    }

    DisposableEffect(state) {
        onDispose {
            state.disposed = true
        }
    }

    decorationBox {
        FlowRow(
            modifier = modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        keyboardController?.show()
                        textFieldFocusRequester.requestFocus()
                        state.currentFocusedChipIndex = -1
                        // Move cursor to the end
                        val selection = state.value.text.length
                        state.value = state.value.copy(selection = TextRange(selection))
                    },
                    enabled = editable,
                ),
            mainAxisSpacing = chipHorizontalSpacing,
            crossAxisSpacing = chipVerticalSpacing,
            crossAxisAlignment = FlowCrossAxisAlignment.Center
        ) {
            Chips(
                state = state,
                enabled = enabled,
                readOnly = readOnly || readOnlyChips,
                onRemoveRequest = { state.removeChip(it) },
                onFocused = { interactionSource.tryEmit(it) },
                onFreeFocus = { interactionSource.tryEmit(FocusInteraction.Unfocus(it)) },
                onGiveUpFocuses = {
                    textFieldFocusRequester.requestFocus()
                    state.currentFocusedChipIndex = -1
                },
                onChipClick = onChipClick,
                onChipLongClick = onChipLongClick,
                textStyle = textStyle,
                chipStyle = chipStyle,
                chipLeadingIcon = chipLeadingIcon,
                chipTrailingIcon = chipTrailingIcon,
            )

            Input(
                state = state,
                onSubmit = onSubmit,
                enabled = enabled,
                readOnly = readOnly,
                isError = isError,
                textStyle = textStyle,
                colors = colors,
                keyboardOptions = keyboardOptions,
                focusRequester = textFieldFocusRequester,
                interactionSource = interactionSource,
                onFocusChange = { isFocused ->
                    if (isFocused) {
                        state.currentFocusedChipIndex = -1
                    }
                },
            )
        }
    }
}

@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@Composable
private fun <T : Chip> Chips(
    state: ChipTextFieldState<T>,
    enabled: Boolean,
    readOnly: Boolean,
    onRemoveRequest: (T) -> Unit,
    onFocused: (FocusInteraction.Focus) -> Unit,
    onFreeFocus: (FocusInteraction.Focus) -> Unit,
    onGiveUpFocuses: () -> Unit,
    onChipClick: ((chip: T) -> Unit)?,
    onChipLongClick: ((chip: T) -> Unit)?,
    textStyle: TextStyle,
    chipStyle: ChipStyle,
    chipLeadingIcon: @Composable (chip: T) -> Unit,
    chipTrailingIcon: @Composable (chip: T) -> Unit
) {
    val chips = state.chips

    val focusRequesters = remember(chips.size) {
        List(chips.size) { FocusRequester() }
    }

    fun focusChip(index: Int) {
        focusRequesters[index].requestFocus()
        val targetChip = chips[index]
        targetChip.textFieldValue = targetChip.textFieldValue.copy(
            selection = TextRange(targetChip.text.length),
        )
    }

    LaunchedEffect(chips, state.currentFocusedChipIndex) {
        val index = state.currentFocusedChipIndex
        if (index in 0..chips.lastIndex) {
            focusRequesters[index].requestFocus()
            onFocused(chips[index].focus)
        } else if (index != -1) {
            onGiveUpFocuses()
        }
    }

    for ((index, chip) in chips.withIndex()) {
        ChipItem(
            focusRequester = focusRequesters[index],
            chip = chip,
            enabled = enabled,
            readOnly = readOnly,
            onRemoveRequest = {
                // Call before removing chip
                onFreeFocus(chip.focus)
                if (chips.size > 1) {
                    focusChip((index - 1).coerceAtLeast(0))
                } else {
                    onGiveUpFocuses()
                }
                onRemoveRequest(chip)
            },
            onFocusNextRequest = {
                onFreeFocus(chip.focus)
                if (index < chips.lastIndex) {
                    focusChip(index + 1)
                } else {
                    onGiveUpFocuses()
                    focusRequesters[index].freeFocus()
                }
            },
            onFocusChange = { isFocused ->
                if (isFocused) {
                    state.currentFocusedChipIndex = index
                    onFocused(chip.focus)
                } else {
                    onFreeFocus(chip.focus)
                }
            },
            onClick = { onChipClick?.invoke(chip) },
            onLongClick = { onChipLongClick?.invoke(chip) },
            textStyle = textStyle,
            chipStyle = chipStyle,
            chipLeadingIcon = chipLeadingIcon,
            chipTrailingIcon = chipTrailingIcon
        )
    }
}

@ExperimentalComposeUiApi
@Composable
private fun <T : Chip> Input(
    state: ChipTextFieldState<T>,
    onSubmit: (() -> Unit)?,
    enabled: Boolean,
    readOnly: Boolean,
    isError: Boolean,
    textStyle: TextStyle,
    colors: TextFieldColors,
    keyboardOptions: KeyboardOptions,
    focusRequester: FocusRequester,
    interactionSource: MutableInteractionSource,
    onFocusChange: (isFocused: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val value = state.value
    if (value.text.isEmpty() && (!enabled || readOnly)) {
        return
    }
    val textColor = textStyle.color.takeOrElse {
        colors.textColor(enabled).value
    }
    BasicTextField(
        value = value,
        onValueChange = filterNewLine { newValue, hasNewLine ->
            state.onValueChange(newValue)
            if (hasNewLine && newValue.text.isNotEmpty()) {
                onSubmit?.invoke()
            }
        },
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusChange(it.isFocused) }
            .onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.Backspace) {
                    if (value.text.isEmpty() && state.chips.isNotEmpty()) {
                        // Remove previous chip
                        state.removeLastChip()
                        return@onPreviewKeyEvent true
                    }
                }
                false
            },
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle.copy(color = textColor),
        keyboardOptions = keyboardOptions.copy(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                if (value.text.isNotEmpty()) {
                    onSubmit?.invoke()
                }
            }
        ),
        interactionSource = interactionSource,
        cursorBrush = SolidColor(colors.cursorColor(isError).value),
    )
}

@ExperimentalComposeUiApi
@ExperimentalFoundationApi
@Composable
private fun <T : Chip> ChipItem(
    focusRequester: FocusRequester,
    chip: T,
    enabled: Boolean,
    readOnly: Boolean,
    onRemoveRequest: () -> Unit,
    onFocusNextRequest: () -> Unit,
    onFocusChange: (isFocused: Boolean) -> Unit,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    textStyle: TextStyle,
    chipStyle: ChipStyle,
    chipLeadingIcon: @Composable (chip: T) -> Unit,
    chipTrailingIcon: @Composable (chip: T) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val shape by chipStyle.shape(
        enabled = enabled,
        readOnly = readOnly,
        interactionSource = interactionSource,
    )

    val borderWidth by chipStyle.borderWidth(
        enabled = enabled,
        readOnly = readOnly,
        interactionSource = interactionSource,
    )

    val borderColor by chipStyle.borderColor(
        enabled = enabled,
        readOnly = readOnly,
        interactionSource = interactionSource,
    )

    val textColor by chipStyle.textColor(
        enabled = enabled,
        readOnly = readOnly,
        interactionSource = interactionSource,
    )
    val chipTextStyle = remember(textColor) { textStyle.copy(color = textColor) }

    val backgroundColor by chipStyle.backgroundColor(
        enabled = enabled,
        readOnly = readOnly,
        interactionSource = interactionSource,
    )

    val keyboardController = LocalSoftwareKeyboardController.current

    val editable = enabled && !readOnly

    DisposableEffect(chip) {
        onDispose {
            onFocusChange(false)
        }
    }

    ChipItemLayout(
        leadingIcon = {
            chipLeadingIcon(chip)
        },
        trailingIcon = {
            chipTrailingIcon(chip)
        },
        modifier = modifier
            .clip(shape = shape)
            .background(color = backgroundColor)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = shape
            )
            .padding(borderWidth)
            .combinedClickable(
                enabled = enabled,
                onClick = {
                    if (editable) {
                        keyboardController?.show()
                        focusRequester.requestFocus()
                    }
                    onClick?.invoke()
                },
                onLongClick = {
                    onLongClick?.invoke()
                }
            ),
    ) {
        var canRemoveChip by remember { mutableStateOf(false) }
        BasicTextField(
            value = chip.textFieldValue,
            onValueChange = filterNewLine { value, hasNewLine ->
                chip.textFieldValue = value
                if (hasNewLine) {
                    onFocusNextRequest()
                }
            },
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .padding(horizontal = 8.dp, vertical = 3.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { onFocusChange(it.isFocused) }
                .onPreviewKeyEvent {
                    if (it.key == Key.Backspace) {
                        if (it.type == KeyEventType.KeyDown) {
                            canRemoveChip = chip.text.isEmpty()
                        } else if (it.type == KeyEventType.KeyUp) {
                            if (canRemoveChip) {
                                onRemoveRequest()
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    false
                },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onFocusNextRequest() }),
            singleLine = false,
            enabled = !readOnly && enabled,
            readOnly = readOnly || !enabled,
            textStyle = chipTextStyle,
            interactionSource = interactionSource
        )
    }
}

@Composable
private fun ChipItemLayout(
    leadingIcon: @Composable () -> Unit,
    trailingIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier,
        content = {
            Box { leadingIcon() }
            Box { content() }
            Box { trailingIcon() }
        },
    ) { measurables, constraints ->
        val maxWidth = constraints.maxWidth

        val leadingMeasurable = measurables[0]
        val contentMeasurable = measurables[1]
        val trailingMeasurable = measurables[2]

        var restWidth = maxWidth

        val leadingPlaceable = leadingMeasurable.measure(constraints = constraints)
        restWidth -= leadingPlaceable.width

        val trailingPlaceable = trailingMeasurable.measure(Constraints(maxWidth = restWidth))
        restWidth -= trailingPlaceable.width

        val contentPlaceable = contentMeasurable.measure(Constraints(maxWidth = restWidth))

        val width = leadingPlaceable.width + contentPlaceable.width + trailingPlaceable.width
        val height = maxOf(
            leadingPlaceable.height,
            contentPlaceable.height,
            trailingPlaceable.height,
        )

        val placeables = arrayOf(leadingPlaceable, contentPlaceable, trailingPlaceable)

        layout(width = width, height = height) {
            var x = 0
            for (placeable in placeables) {
                placeable.placeRelative(
                    x = x,
                    y = (height - placeable.height) / 2,
                )
                x += placeable.width
            }
        }
    }
}
