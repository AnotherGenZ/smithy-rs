/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.rust.codegen.server.smithy.generators

import software.amazon.smithy.model.knowledge.TopDownIndex
import software.amazon.smithy.rust.codegen.rustlang.RustModule
import software.amazon.smithy.rust.codegen.server.smithy.generators.protocol.ServerProtocolTestGenerator
import software.amazon.smithy.rust.codegen.smithy.CodegenContext
import software.amazon.smithy.rust.codegen.smithy.RustCrate
import software.amazon.smithy.rust.codegen.smithy.customize.RustCodegenDecorator
import software.amazon.smithy.rust.codegen.smithy.generators.error.errorSymbol
import software.amazon.smithy.rust.codegen.smithy.generators.protocol.ProtocolGenerator
import software.amazon.smithy.rust.codegen.smithy.generators.protocol.ProtocolSupport

/**
 * ServerServiceGenerator
 *
 * Service generator is the main codegeneration entry point for Smithy services. Individual structures and unions are
 * generated in codegen visitor, but this class handles all protocol-specific code generation (i.e. operations).
 */
class ServerServiceGenerator(
    private val rustCrate: RustCrate,
    private val protocolGenerator: ProtocolGenerator,
    private val protocolSupport: ProtocolSupport,
    private val context: CodegenContext,
    private val decorator: RustCodegenDecorator,
) {
    private val index = TopDownIndex.of(context.model)

    /**
     * Render Service Specific code. Code will end up in different files via [useShapeWriter]. See `SymbolVisitor.kt`
     * which assigns a symbol location to each shape.
     */
    fun render() {
        val operations = index.getContainedOperations(context.serviceShape).sortedBy { it.id }
        operations.map { operation ->
            rustCrate.useShapeWriter(operation) { operationWriter ->
                protocolGenerator.serverRenderOperation(
                    operationWriter,
                    operation,
                    decorator.operationCustomizations(context, operation, listOf())
                )
                ServerProtocolTestGenerator(context, protocolSupport, operation, operationWriter)
                    .render()
            }

            if (operation.errors.isNotEmpty()) {
                rustCrate.withModule(RustModule.Error) { writer ->
                    ServerCombinedErrorGenerator(context.model, context.symbolProvider, operation)
                        .render(writer)
                }
            }
        }
    }
}