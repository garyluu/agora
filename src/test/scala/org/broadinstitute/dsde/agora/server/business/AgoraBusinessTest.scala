package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.AgoraTestData._
import org.broadinstitute.dsde.agora.server.exceptions.NamespaceAuthorizationException
import org.broadinstitute.dsde.agora.server.model.AgoraEntityType
import org.scalatest.{DoNotDiscover, FlatSpec, Matchers}

@DoNotDiscover
class AgoraBusinessTest extends FlatSpec with Matchers {

  val agoraBusiness = new AgoraBusiness()
  val methodImportResolver = new MethodImportResolver(agoraCIOwner.get, agoraBusiness)

  "Agora" should "not find a method payload when resolving a WDL import statement if the method has not been added" in {
    val importString = "methods://broad.nonexistent.5400"
    intercept[Exception] {
      methodImportResolver.importResolver(importString)
    }
  }

  "Agora" should "throw an exception when trying to resolve a WDL import that is improperly formatted" in {
    val importString = "methods:broad.nonexistent.5400"
    intercept[Exception] {
      methodImportResolver.importResolver(importString)
    }
  }

  "Agora" should "not let users without permissions redact a method" in {
    val testEntityToBeRedactedWithId3 = agoraBusiness.find(testEntityToBeRedacted3, None, Seq(testEntityToBeRedacted3.entityType.get), mockAutheticatedOwner.get).head
    intercept[NamespaceAuthorizationException] {
      val rowsEdited: Int = agoraBusiness.delete(testEntityToBeRedactedWithId3, AgoraEntityType.MethodTypes, owner2.get)
    }
  }

  "Agora" should "allow admin users to redact any method" in {
    val testEntityToBeRedactedWithId3 = agoraBusiness.find(testEntityToBeRedacted3, None, Seq(testEntityToBeRedacted3.entityType.get), mockAutheticatedOwner.get).head
    val rowsEdited: Int = agoraBusiness.delete(testEntityToBeRedactedWithId3, AgoraEntityType.MethodTypes, adminUser.get)
    assert(rowsEdited === 1)
  }


}
