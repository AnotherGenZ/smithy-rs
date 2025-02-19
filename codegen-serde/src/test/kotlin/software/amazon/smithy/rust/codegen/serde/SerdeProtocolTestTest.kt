/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rust.codegen.serde

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ValueSource
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.SourceLocation
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.transform.ModelTransformer
import software.amazon.smithy.rust.codegen.client.testutil.clientIntegrationTest
import software.amazon.smithy.rust.codegen.core.smithy.generators.protocol.ServiceShapeId
import software.amazon.smithy.rust.codegen.core.testutil.IntegrationTestParams
import software.amazon.smithy.rust.codegen.core.testutil.asSmithyModel
import software.amazon.smithy.rust.codegen.core.util.letIf
import software.amazon.smithy.rust.codegen.server.smithy.testutil.serverIntegrationTest
import java.io.File
import java.util.stream.Stream

class SerdeProtocolTestTest {
    companion object {
        @JvmStatic
        fun testedModels(): Stream<Arguments> =
            Stream.of(
                Arguments.of(ShapeId.from(ServiceShapeId.REST_JSON)),
                Arguments.of(ShapeId.from("com.amazonaws.constraints#ConstraintService")),
            )
    }

    @Test
    fun testRestJson() {
        val serviceShapeId = ShapeId.from("com.amazonaws.constraints#ConstraintService")
        val model = Model.assembler().discoverModels().assemble().result.get().attachSerdeToService(serviceShapeId)
        clientIntegrationTest(
            model,
            IntegrationTestParams(service = serviceShapeId.toString(), cargoCommand = "cargo test --all-features"),
        ) { clientCodegenContext, rustCrate ->
        }
    }

    private fun Model.attachSerdeToService(serviceShapeId: ShapeId): Model {
        val service =
            this.expectShape(serviceShapeId, ServiceShape::class.java).toBuilder().addTrait(
                SerdeTrait(true, false, null, null, SourceLocation.NONE),
            ).build()
        return ModelTransformer.create().mapShapes(this) { serviceShape ->
            serviceShape.letIf(serviceShape.id == serviceShapeId) {
                service
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testConstraintsModel(usePublicConstrainedTypes: Boolean) {
        val constraintsService = ShapeId.from("com.amazonaws.constraints#ConstraintsService")
        val filePath = "../codegen-core/common-test-models/constraints.smithy"
        val model = File(filePath).readText().asSmithyModel().attachSerdeToService(constraintsService)
        val constrainedShapesSettings =
            Node.objectNodeBuilder().withMember(
                "codegen",
                Node.objectNodeBuilder()
                    .withMember("publicConstrainedTypes", usePublicConstrainedTypes)
                    .build(),
            ).build()
        serverIntegrationTest(
            model,
            IntegrationTestParams(
                service = constraintsService.toString(),
                cargoCommand = "cargo test --all-features",
                additionalSettings = constrainedShapesSettings,
            ),
        ) { clientCodegenContext, rustCrate ->
        }
    }
}
