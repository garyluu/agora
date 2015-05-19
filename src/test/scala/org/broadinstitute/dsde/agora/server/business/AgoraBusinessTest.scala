package org.broadinstitute.dsde.agora.server.business

import org.broadinstitute.dsde.agora.server.model.AgoraEntity
import org.scalatest.{Matchers, FlatSpec, DoNotDiscover}

/**
 * Created by dshiga on 5/14/15.
 */
@DoNotDiscover
class AgoraBusinessTest extends FlatSpec with Matchers {

  "Agora" should "return an empty URL if entity namespace, name, or snapshotId are missing" in {
    val noNamespace = AgoraEntity(name = Option("test"), snapshotId = Option(12))
    val blankName = AgoraEntity(namespace = Option("broad"), name = Option("   "), snapshotId = Option(12))
    val noSnapshotId = AgoraEntity(namespace = Option("broad"), name = Option("test"))
    assert(AgoraBusiness.agoraUrl(noNamespace) === "")
    assert(AgoraBusiness.agoraUrl(blankName) === "")
    assert(AgoraBusiness.agoraUrl(noSnapshotId) === "")
  }

  "Agora" should "return a URL given an entity with a namespace, name, and id" in {
    val entity = AgoraEntity(namespace = Option("broad"), name = Option("test"), snapshotId = Option(12))
    assert(AgoraBusiness.agoraUrl(entity) === "http://localhost:8000/methods/broad/test/12")
  }

  "Agora" should "not find a method payload when resolving a WDL import statement if the method has not been added" in {
    val importString = "methods://broad.nonexistent.5400"
    assert(AgoraBusiness.importResolver(importString) === "")
  }

  "Agora" should "throw an exception when trying to resolve a WDL import that is improperly formatted" in {
    val importString = "methods:broad.nonexistent.5400"
    intercept[Exception] {
      AgoraBusiness.importResolver(importString)
    }
  }
}
