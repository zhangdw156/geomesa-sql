package com.spatialx.geomesa.sql

import org.geotools.data.DataStore
import org.junit.runner.RunWith
import org.specs2.execute.Result
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.geotools.data.Query
import org.geotools.data.Transaction
import org.locationtech.geomesa.index.conf.QueryHints
import scala.util.matching.Regex
import org.geotools.data.FeatureReader
import org.opengis.feature.simple.SimpleFeature
import org.opengis.feature.simple.SimpleFeatureType

@RunWith(classOf[JUnitRunner])
class StatTest extends Specification {
  val ds: DataStore = PrepareTestDataStore.ds
  "StatTest" should {
    "test" in {
      verify()
      success
    }
  }
  private def verify() {
    val query: Query = new Query("test_data")
    query.getHints.put(QueryHints.STATS_STRING, "Count()")
    val reader: FeatureReader[SimpleFeatureType, SimpleFeature] = ds.getFeatureReader(query, Transaction.AUTO_COMMIT)
    while (reader.hasNext) {
      println(s"reader.next(): ${ reader.next}")
    }
  }

}