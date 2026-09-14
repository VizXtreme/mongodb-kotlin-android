package com.vizx.mongodbclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vizx.mongodbclient.data.FieldValueType
import com.vizx.mongodbclient.data.QueryFilterRule
import com.vizx.mongodbclient.data.QueryOperator

@Composable
fun VisualQueryBuilderCard(
    rules: List<QueryFilterRule>,
    onAddRule: () -> Unit,
    onUpdateRule: (QueryFilterRule) -> Unit,
    onRemoveRule: (String) -> Unit,
    onClearRules: () -> Unit,
    onApplyGeneratedFilter: (String) -> Unit
) {
    val generatedJson = remember(rules) { buildMongoFilterJson(rules) }

    SkeletonCard(
        title = "Visual Query Builder (${rules.size} conditions)",
        trailingAction = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "[+ ADD CONDITION]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = SkeletonTheme.Success,
                    modifier = Modifier.clickable { onAddRule() }
                )
                if (rules.isNotEmpty()) {
                    Text(
                        text = "[CLEAR]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = SkeletonTheme.Error,
                        modifier = Modifier.clickable { onClearRules() }
                    )
                }
            }
        }
    ) {
        if (rules.isEmpty()) {
            Text(
                text = "No visual conditions configured. Click [+ ADD CONDITION] to build queries visually without typing raw JSON.",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = SkeletonTheme.TextDisabled
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rules.forEachIndexed { index, rule ->
                    RuleEditorRow(
                        rule = rule,
                        index = index + 1,
                        onUpdate = onUpdateRule,
                        onRemove = { onRemoveRule(rule.id) }
                    )
                }

                // Live Generated JSON Preview
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SkeletonTheme.SurfaceElevated, RectangleShape)
                        .border(1.dp, SkeletonTheme.BorderFocused, RectangleShape)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GENERATED MONGO FILTER:",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = SkeletonTheme.TextSecondary
                        )
                        Text(
                            text = "[USE THIS FILTER]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = SkeletonTheme.Success,
                            modifier = Modifier.clickable { onApplyGeneratedFilter(generatedJson) }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = generatedJson,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = SkeletonTheme.TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun RuleEditorRow(
    rule: QueryFilterRule,
    index: Int,
    onUpdate: (QueryFilterRule) -> Unit,
    onRemove: () -> Unit
) {
    var opDropdownOpen by remember { mutableStateOf(false) }
    var typeDropdownOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SkeletonTheme.SurfaceElevated, RectangleShape)
            .border(1.dp, SkeletonTheme.Border, RectangleShape)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#$index Condition",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = SkeletonTheme.TextSecondary
            )
            Text(
                text = "[DELETE]",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = SkeletonTheme.Error,
                modifier = Modifier.clickable { onRemove() }
            )
        }

        // Field and Value inputs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SkeletonTextField(
                value = rule.field,
                onValueChange = { onUpdate(rule.copy(field = it)) },
                label = "Field (e.g. status)",
                singleLine = true,
                modifier = Modifier.weight(1f)
            )

            SkeletonTextField(
                value = rule.value,
                onValueChange = { onUpdate(rule.copy(value = it)) },
                label = "Value",
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        // Operator selector and Value Type selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Operator dropdown trigger
            Box(modifier = Modifier.weight(1.5f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SkeletonTheme.Surface, RectangleShape)
                        .border(1.dp, SkeletonTheme.Border, RectangleShape)
                        .clickable { opDropdownOpen = true }
                        .padding(6.dp)
                ) {
                    Text(
                        text = "OPERATOR",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = SkeletonTheme.TextSecondary
                    )
                    Text(
                        text = rule.operator.label,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = SkeletonTheme.TextPrimary
                    )
                }

                DropdownMenu(
                    expanded = opDropdownOpen,
                    onDismissRequest = { opDropdownOpen = false }
                ) {
                    QueryOperator.entries.forEach { op ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = op.label,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            },
                            onClick = {
                                onUpdate(rule.copy(operator = op))
                                opDropdownOpen = false
                            }
                        )
                    }
                }
            }

            // Value Type dropdown trigger
            Box(modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SkeletonTheme.Surface, RectangleShape)
                        .border(1.dp, SkeletonTheme.Border, RectangleShape)
                        .clickable { typeDropdownOpen = true }
                        .padding(6.dp)
                ) {
                    Text(
                        text = "TYPE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = SkeletonTheme.TextSecondary
                    )
                    Text(
                        text = rule.valueType.label,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = SkeletonTheme.TextPrimary
                    )
                }

                DropdownMenu(
                    expanded = typeDropdownOpen,
                    onDismissRequest = { typeDropdownOpen = false }
                ) {
                    FieldValueType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = type.label,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            },
                            onClick = {
                                onUpdate(rule.copy(valueType = type))
                                typeDropdownOpen = false
                            }
                        )
                    }
                }
            }
        }
    }
}

fun buildMongoFilterJson(rules: List<QueryFilterRule>): String {
    val validRules = rules.filter { it.field.isNotBlank() }
    if (validRules.isEmpty()) return "{}"

    val entries = validRules.map { rule ->
        val field = rule.field.trim()
        val rawVal = rule.value.trim()

        val formattedVal = when (rule.valueType) {
            FieldValueType.NUMBER -> rawVal.toDoubleOrNull()?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: "\"$rawVal\""
            FieldValueType.BOOLEAN -> (rawVal.lowercase() == "true").toString()
            FieldValueType.STRING -> when (rule.operator) {
                QueryOperator.IN_ARRAY -> {
                    val items = rawVal.split(",").map { "\"${it.trim()}\"" }
                    "[${items.joinToString(", ")}]"
                }
                else -> "\"$rawVal\""
            }
        }

        val opClause = when (rule.operator) {
            QueryOperator.EQUALS -> formattedVal
            QueryOperator.CONTAINS_TEXT -> "{\"\$regex\": \"$rawVal\", \"\$options\": \"i\"}"
            else -> "{\"${rule.operator.mongoOp}\": $formattedVal}"
        }

        "  \"$field\": $opClause"
    }

    return "{\n${entries.joinToString(",\n")}\n}"
}
