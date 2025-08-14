# GeoMesa SQL 源码解析

本文档对 `geomesa-sql-core/src/main/scala/com/spatialx/geomesa/sql` 目录下的源码进行解析，不包括子文件夹中的内容。

## 1. package.scala

这个文件定义了 `com.spatialx.geomesa.sql` 包对象，包含了一个常量：

\`\`\`scala
val FID_FIELD_NAME = "__FID__"
\`\`\`

这个常量定义了特征ID的列/字段名称。由于 SimpleFeature 访问特征ID的方法与访问其他属性的方法有很大不同，因此需要对特征ID列进行特殊处理。

## 2. GeoMesaTable.scala

`GeoMesaTable` 是一个抽象基类，用于将 GeoMesa DataStore 适配到 Calcite。主要功能包括：

1. 从 SimpleFeatureType 填充表的行类型，并添加一个额外的 `__FID__` 字段用于特征ID
2. 提供将 SimpleFeatureType 转换为 Calcite 行类型的方法
3. 包含将各种 GeoTools 数据类型映射到 Calcite SQL 类型的逻辑

关键方法：
- `getRowType`: 获取表的行类型
- `getFieldTypes`: 获取字段类型，便于实现 Table 接口
- `getSimpleFeatureType`: 获取 GeoMesa 表的 SimpleFeatureType

`GeoMesaTable` 对象包含了一系列辅助方法，用于将 GeoTools 的 AttributeDescriptor 转换为 Calcite 的结构字段，以及处理各种数据类型的映射关系。

## 3. GeoMesaSchema.scala

`GeoMesaSchema` 类用于发现存储在 GeoTools DataStore 中的 SimpleFeatureType，并将其作为表填充。主要特点：

1. 继承自 AbstractSchema，用于表示 Calcite 中的一个 Schema
2. 使用 Caffeine 缓存来提高性能，缓存过期时间可配置
3. 通过 `getTableMap` 方法获取 Schema 中的所有表（即 DataStore 中的所有 SimpleFeatureType）

关键方法：
- `getTableMap`: 获取所有表的映射
- `createTable`: 创建表实例
- `populateTableMap`: 填充表映射

## 4. GeoMesaQueryParams.scala

`GeoMesaQueryParams` 是一个 case class，用于封装查询 GeoMesa DataStore 的参数。包括：

1. 表名（typeName）
2. ECQL 查询字符串
3. 属性列表
4. 统计字符串和属性
5. 分页参数（offset 和 fetch）

关键方法：
- `toExpression`: 将参数转换为 Calcite 表达式
- `toQuery`: 将参数转换为 GeoTools Query 对象

这个类在 GeoMesa SQL 查询优化中起着重要作用，允许将 SQL 查询转换为 GeoMesa 原生查询。

## 5. GeoMesaTableFactory.scala

`GeoMesaTableFactory` 类是一个工厂类，用于创建 GeoMesaTable 实例。主要功能：

1. 实现 TableFactory 接口，允许用户直接访问 GeoMesa Schema，跳过 Schema 发现过程
2. 根据配置参数创建不同类型的 GeoMesaTable 实例

关键方法：
- `create`: 创建 GeoMesaTable 实例
- `createTable`: 根据表类型创建不同的 GeoMesaTable 实现（scannable 或 translatable）

## 6. GeoMesaSchemaFactory.scala

`GeoMesaSchemaFactory` 类是一个工厂类，用于实例化 Schema 对象，以访问 GeoMesa DataStore 中的 SimpleFeatureType。这是发现 GeoMesa DataStore 中表和行的入口点。

主要特点：
1. 实现 SchemaFactory 接口
2. 使用 Caffeine 缓存来提高性能
3. 提供 DataStore 对象的创建和缓存

关键方法：
- `create`: 创建 Schema 实例
- `getOrCreateDataStore`: 创建或获取已创建的 DataStore 对象

## 7. GeoMesaScannableTable.scala

`GeoMesaScannableTable` 类实现了 ScannableTable 接口，只支持从 DataStore 顺序扫描数据，不利用 GeoMesa 索引。这是一个基准实现，用于验证其他更优化的 Table 接口实现的正确性。

关键方法：
- `scan`: 扫描表中的数据，返回一个 Enumerable 对象

这个类使用 SimpleFeatureEnumerator 来遍历 SimpleFeature，并使用 SimpleFeatureArrayConverter 将其转换为数组。

## 8. GeoMesaTranslatableTable.scala

`GeoMesaTranslatableTable` 类是一个带有查询优化规则的 GeoMesa 表实现。它重写查询以将谓词、投影和聚合下推到 GeoTools 过滤器。

主要特点：
1. 实现 QueryableTable、TranslatableTable 和 ModifiableTable 接口
2. 支持查询优化
3. 支持表修改操作

关键方法：
- `query`: 在 GeoMesa Schema 上运行 GeoTools 查询
- `asQueryable`: 将表转换为 Queryable 对象
- `toRel`: 将表转换为关系节点
- `toModificationRel`: 将表转换为修改关系节点

内部类 `GeoMesaQueryable` 基于 GeoMesaTranslatableTable 实现了 Queryable 接口，用于支持查询操作。

这个类是 GeoMesa SQL 查询优化的核心，它通过生成逻辑和物理计划节点，实现了 SQL 查询到 GeoMesa 查询的高效转换。