# GeoMesa SQL Rules 源码解析

本文档对 `geomesa-sql-core/src/main/scala/com/spatialx/geomesa/sql/rules` 目录下的源码进行解析。

## 1. GeoMesaRules.scala

`GeoMesaRules` 是一个对象，定义了 GeoMesa SQL 中使用的所有优化规则。它将规则分为三类：

1. `COMMON_LOGICAL_RULES`：常用的逻辑规则，包括表扫描转换、过滤下推、投影下推、限制下推和连接优化等规则。
2. `AGGREGATION_RULE`：聚合规则，用于聚合操作的优化。
3. `PHYSICAL_RULES`：物理规则，用于将 GeoMesa 关系转换为可枚举关系。

这个文件是整个规则系统的入口点，汇总了所有可用的优化规则。

## 2. GeoMesaLimitRule.scala

`GeoMesaLimitRule` 实现了将 LIMIT 子句下推到 GeoMesa 表扫描的优化规则。主要功能：

1. 检测 LogicalSort 操作中的 offset 和 fetch 参数（对应 SQL 中的 OFFSET 和 LIMIT）
2. 将这些参数直接应用到 GeoMesaLogicalTableScan 中，避免不必要的数据扫描

关键方法：
- `onMatch`：匹配规则并应用转换
- `convert`：将限制参数应用到表扫描中
- `rexNodeToIntValue`：将 RexNode 转换为整数值

## 3. GeoMesaFilterRule.scala

`GeoMesaFilterRule` 实现了将过滤条件下推到 GeoMesa 表扫描的优化规则。主要功能：

1. 分析 LogicalFilter 中的条件表达式
2. 尝试将条件转换为 GeoTools 过滤器
3. 将可转换的条件下推到 GeoMesa 表扫描中，保留无法转换的条件在上层处理

关键方法：
- `onMatch`：匹配规则并应用转换
- `convert`：将过滤条件转换并应用到表扫描中

这个规则使用 `RexNodeTranslator` 将 Calcite 的 RexNode 表达式转换为 GeoTools 的过滤器。

## 4. RexNodeTranslator.scala

`RexNodeTranslator` 是一个核心工具类，负责将 Calcite 的 RexNode 表达式转换为 GeoTools 的过滤器和表达式。这是实现查询优化的关键组件。主要功能：

1. 支持各种 SQL 操作符的转换，如 AND、OR、NOT、比较运算符等
2. 支持空值检查（IS NULL、IS NOT NULL）
3. 支持 LIKE 操作符
4. 支持 SEARCH 操作（范围查询）
5. 支持空间函数（ST_INTERSECTS、ST_EQUALS 等）

关键方法：
- `rexNodeToGtFilter`：将 RexNode 转换为 GeoTools 过滤器
- `rexNodeToGtExpr`：将 RexNode 转换为 GeoTools 表达式
- `binaryComparisonToGtFilter`：处理二元比较操作
- `udfToGtFilter`：处理用户定义函数（主要是空间函数）
- `searchNodeToGtFilter`：处理 SEARCH 节点（范围查询）

这个类是 GeoMesa SQL 查询优化的核心，它使得 SQL 查询能够有效地利用 GeoMesa 的索引和过滤能力。

## 5. GeoMesaProjectRule.scala

`GeoMesaProjectRule` 实现了将投影操作下推到 GeoMesa 表扫描的优化规则。主要功能：

1. 分析 LogicalProject 中的投影表达式
2. 确定需要的列索引
3. 将投影信息应用到 GeoMesaLogicalTableScan 中，减少不必要的数据读取

关键方法：
- `onMatch`：匹配规则并应用转换
- `convert`：将投影信息应用到表扫描中

这个规则只处理简单的列引用投影，不处理复杂表达式。

## 6. GeoMesaAggregateRule.scala

`GeoMesaAggregateRule` 实现了将聚合操作下推到 GeoMesa 表扫描的优化规则。主要功能：

1. 支持 GROUP BY 操作（目前仅支持按单个属性分组）
2. 支持聚合函数，如 COUNT、MIN、MAX 等
3. 将聚合操作转换为 GeoMesa 的统计查询

关键方法：
- `onMatch`：匹配规则并应用转换
- `convert`：将聚合信息应用到表扫描中
- `enumStatSpecs`：生成枚举统计规格
- `aggCallsToStatsSpecs`：将聚合调用转换为统计规格
- `aggCallToStatSpec`：将单个聚合调用转换为统计规格
- `resolveGroupedAttribute`：解析分组属性

这个规则利用了 GeoMesa 的统计功能，可以在数据存储层面高效地执行聚合操作。

## 7. EnumerableSpatialJoinRule.scala

`EnumerableSpatialJoinRule` 实现了将空间连接操作转换为专用的空间连接执行计划的规则。主要功能：

1. 检测连接条件中的空间谓词（如 ST_INTERSECTS、ST_CONTAINS 等）
2. 分析连接操作的左右输入，确定空间索引配置
3. 创建 EnumerableSpatialJoin 节点，利用空间索引加速连接操作

关键方法：
- `convert`：将 LogicalJoin 转换为 EnumerableSpatialJoin
- `analyzeSpatialJoinPredicate`：分析空间连接谓词
- `resolveSpatialJoinRexNodes`：解析空间连接的 RexNode
- `analyzeSideOfInputRefs`：分析输入引用的来源（左侧或右侧）
- `resolveSpatialJoinOperands`：解析空间连接操作数
- `resolveSpatialJoinOperandsForDWithin`：解析 ST_DWITHIN 操作的操作数

这个规则使得空间连接操作能够利用空间索引进行优化，大大提高了空间连接的性能。

## 8. GeoMesaIndexLookupJoinRule.scala

`GeoMesaIndexLookupJoinRule` 实现了通过索引查找优化等值连接的规则。主要功能：

1. 检测等值连接条件
2. 确认右侧表的连接键是否有索引
3. 创建 GeoMesaIndexLookupJoin 节点，利用索引加速连接操作

关键方法：
- `onMatch`：匹配规则并应用转换
- `convert`：将 LogicalJoin 转换为 GeoMesaIndexLookupJoin
- `isEquiKeysIndexed`：检查等值连接键是否有索引
- `getTableIndexSpecs`：获取表的索引规格

这个规则使得等值连接操作能够利用 GeoMesa 的属性索引进行优化，提高了连接操作的性能。

## 9. GeoMesaToEnumerableConverterRule.scala

`GeoMesaToEnumerableConverterRule` 实现了将 GeoMesa 关系转换为可枚举关系的规则。主要功能：

1. 将 GeoMesaRel.CONVENTION 转换为 EnumerableConvention
2. 创建 GeoMesaToEnumerableConverter 节点，桥接 GeoMesa 和 Calcite 的执行引擎

关键方法：
- `convert`：将 GeoMesa 关系转换为可枚举关系

这个规则是 GeoMesa SQL 与 Calcite 执行引擎集成的关键点，使得 GeoMesa 的查询结果能够被 Calcite 处理。

## 10. GeoMesaTableLogicalToPhysicalRule.scala

`GeoMesaTableLogicalToPhysicalRule` 包含了三个规则，用于将逻辑表操作转换为物理表操作：

1. `GeoMesaTableLogicalToPhysicalScanRule`：将 GeoMesaLogicalTableScan 转换为 GeoMesaPhysicalTableScan
2. `GeoMesaTableLogicalToPhysicalModifyRule`：将 GeoMesaLogicalTableModify 转换为 GeoMesaPhysicalTableModify
3. `GeoMesaTablePhysicalModifyRule`：将 LogicalTableModify 转换为 GeoMesaPhysicalTableModify（作为后备规则）

这些规则是逻辑计划到物理计划转换的关键，确保了查询和修改操作能够正确地执行。

## 总结

GeoMesa SQL 的规则系统实现了一系列优化，包括：

1. 谓词下推（过滤条件下推到数据源）
2. 投影下推（只读取需要的列）
3. 聚合下推（利用 GeoMesa 的统计功能）
4. 限制下推（减少数据扫描量）
5. 连接优化（利用空间索引和属性索引）
6. 逻辑到物理计划的转换

这些优化规则使得 SQL 查询能够高效地利用 GeoMesa 的特性，提供了良好的查询性能。