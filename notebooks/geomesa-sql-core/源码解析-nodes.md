# GeoMesa SQL Nodes 源码解析

本文档对 `geomesa-sql-core/src/main/scala/com/spatialx/geomesa/sql/nodes` 目录下的源码进行解析。

## 1. GeoMesaRel.scala

`GeoMesaRel` 是一个特质（trait），代表使用 GeoMesa 调用约定的关系表达式。

主要特点：
- 定义了 GeoMesa 关系节点的基本接口
- 包含 `implement()` 方法，由子类实现，用于将自身转换为 GeoTools Query 对象
- 定义了 GeoMesa 的调用约定（CONVENTION）
- 定义了 `Result` 类，作为实现过程的返回类型，包含表、GeoMesa表和查询参数

这个特质是 GeoMesa SQL 关系代数操作的基础，所有 GeoMesa 物理操作符都实现了这个特质。

## 2. GeoMesaTableScan.scala

`GeoMesaTableScan` 是一个抽象类，表示 GeoMesa 表的扫描操作。它可能包含被下推到表扫描关系的投影和过滤器。

主要特点：
- 继承自 Calcite 的 `TableScan` 类
- 包含 `scanParams` 参数，封装了扫描操作的各种参数（过滤条件、投影列、聚合、分页等）
- 提供了 `propertyNameList` 方法，用于获取属性名列表
- 重写了 `explainTerms` 方法，用于解释查询计划
- 提供了判断是否有聚合或分页的辅助方法

`ScanParams` 内部类定义了表扫描的参数，包括：
- 过滤条件（Calcite 表达式和 GeoTools 过滤器）
- 投影列索引列表
- 结果行类型
- 聚合统计字符串和属性
- 分页参数（offset 和 fetch）

## 3. EnumerableSpatialJoin.scala

`EnumerableSpatialJoin` 类是一个针对空间谓词优化的连接实现。

主要特点：
- 继承自 Calcite 的 `Join` 类，实现了 `EnumerableRel` 接口
- 使用空间索引配置（`SpatialIndexConfig`）来优化空间连接
- 重写了 `implement` 方法，生成执行空间连接的代码
- 包含 `spatialOperandExpression` 方法，用于生成评估空间操作数的表达式
- 定义了 `spatialJoin` 静态方法，实际执行空间连接操作

`SpatialIndexConfig` 内部类定义了构建和查询空间索引的配置，包括：
- 左侧关系的几何表达式
- 右侧关系的几何表达式
- 距离参数（用于支持 dwithin 连接）

这个类通过使用空间索引来加速空间连接操作，是 GeoMesa SQL 空间查询优化的重要组成部分。

## 4. GeoMesaIndexLookupJoin.scala

`GeoMesaIndexLookupJoin` 类是 GeoTools 数据存储的索引查找连接操作符。

主要特点：
- 继承自 Calcite 的 `Join` 类，实现了 `EnumerableRel` 接口
- 针对 GeoMesa 索引优化的连接操作
- 重写了 `implement` 方法，生成执行索引查找连接的代码
- 定义了 `indexLookupJoin` 静态方法，实际执行索引查找连接操作

这个类利用 GeoMesa 的索引能力来加速连接操作，通过直接查找索引而不是全表扫描来提高性能。

## 5. GeoMesaLogicalTableScan.scala

`GeoMesaLogicalTableScan` 类是 `GeoMesaTableScan` 的逻辑版本。所有逻辑转换（如过滤器/投影下推）都在这个节点上执行。

主要特点：
- 继承自 `GeoMesaTableScan` 类
- 提供了一系列 `with*` 方法（如 `withFilter`、`withProject`、`withAggregate`、`withLimit`），用于应用各种操作
- 包含 `toPhysical` 方法，将逻辑扫描转换为物理扫描
- 重写了 `register` 方法，注册适用的优化规则

这个类是 GeoMesa SQL 查询优化的核心，它允许将各种操作（过滤、投影、聚合、分页）下推到 GeoMesa 数据存储，从而提高查询性能。

## 6. GeoMesaPhysicalTableScan.scala

`GeoMesaPhysicalTableScan` 类是 `GeoMesaTableScan` 的物理版本，由转换规则 `GeoMesaTableLogicalToPhysicalRule` 从 `GeoMesaLogicalTableScan` 转换而来。

主要特点：
- 继承自 `GeoMesaTableScan` 类，实现了 `GeoMesaRel` 接口
- 重写了 `computeSelfCost` 方法，计算自身成本（考虑过滤器、列裁剪、聚合和分页的影响）
- 重写了 `estimateRowCount` 方法，估计行数
- 实现了 `implement` 方法，将自身转换为 GeoMesa 查询参数

这个类是 GeoMesa SQL 查询执行的核心，它负责将逻辑查询计划转换为实际的 GeoMesa 查询。

## 7. GeoMesaLogicalTableModify.scala

`GeoMesaLogicalTableModify` 类是修改 GeoMesa 表的逻辑计划节点。

主要特点：
- 继承自 Calcite 的 `TableModify` 类
- 支持插入、更新和删除操作
- 包含 `toPhysical` 方法，将逻辑修改转换为物理修改
- 重写了 `register` 方法，注册适用的优化规则

这个类是 GeoMesa SQL 数据修改操作的逻辑表示，它定义了如何修改 GeoMesa 表的数据。

## 8. GeoMesaPhysicalTableModify.scala

`GeoMesaPhysicalTableModify` 类是修改 GeoMesa 表的物理计划节点，由 `GeoMesaLogicalTableModify` 转换而来。

主要特点：
- 继承自 Calcite 的 `TableModify` 类，实现了 `EnumerableRel` 接口
- 支持插入、更新和删除操作
- 重写了 `implement` 方法，生成执行表修改的代码
- 包含 `projectChildExpression` 和 `buildChildExpressionProjector` 方法，用于处理表达式投影
- 定义了 `modifyTable` 静态方法，实际执行表修改操作

这个类是 GeoMesa SQL 数据修改操作的物理实现，它负责将逻辑修改计划转换为实际的 GeoMesa 数据修改操作。

## 9. GeoMesaToEnumerableConverter.scala

`GeoMesaToEnumerableConverter` 类是一个转换器，将 GeoMesa 关系表达式转换为可枚举的关系表达式。

主要特点：
- 继承自 Calcite 的 `ConverterImpl` 类，实现了 `EnumerableRel` 接口
- 重写了 `implement` 方法，生成执行 GeoMesa 查询的代码
- 将 GeoMesa 关系转换为可枚举的结果集

这个类是 GeoMesa SQL 查询执行的最后一步，它将 GeoMesa 查询结果转换为 Calcite 可以处理的可枚举结果集，从而完成整个查询过程。