package org.broadinstitute.dsde.agora.server.dataaccess.mongo

import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.broadinstitute.dsde.agora.server.{AgoraDbTest, AgoraTestData}
import org.scalatest.{DoNotDiscover, FlatSpec}

@DoNotDiscover
class MethodsDbTest extends FlatSpec with AgoraDbTest with AgoraTestData {

  "Agora" should "be able to store a method" in {
    agoraDao.insert(testEntity1)

    val entity = agoraDao.findSingle(testEntity1).get

    assert(entity == testEntity1)
  }

  "Agora" should "be able to query by namespace, name and version and get back a single entity" in {
    //NB: agoraTestMethod has already been stored.
    val queryEntity = new AgoraEntity(namespace = namespace1, name = name1, id = testEntity1.id)

    val entity = agoraDao.findSingle(queryEntity).get

    assert(entity == testEntity1)
  }

  "Agora" should "increment the id number if we insert the same namespace/name entity" in {
    agoraDao.insert(testEntity1)

    val previousVersionEntity = testEntity1.copy()
    previousVersionEntity.id = Option(testEntity1.id.get - 1)

    val entity1 = agoraDao.findSingle(previousVersionEntity).get
    val entity2 = agoraDao.findSingle(testEntity1).get

    assert(entity1.id.get == entity2.id.get - 1)
  }
}
