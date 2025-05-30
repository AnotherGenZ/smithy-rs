/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

use std::borrow::Cow;

use aws_smithy_runtime_api::client::auth::static_resolver::StaticAuthSchemeOptionResolver;
use aws_smithy_runtime_api::client::auth::AuthSchemeId;
use aws_smithy_runtime_api::client::runtime_components::RuntimeComponentsBuilder;
use aws_smithy_runtime_api::client::runtime_plugin::{Order, RuntimePlugin};

// A runtime plugin that registers `StaticAuthSchemeOptionResolver` with `RuntimeComponents`.
#[derive(Debug)]
pub(crate) struct DefaultAuthOptionsPlugin {
    runtime_components: RuntimeComponentsBuilder,
}

impl DefaultAuthOptionsPlugin {
    pub(crate) fn new(auth_schemes: Vec<AuthSchemeId>) -> Self {
        let runtime_components = RuntimeComponentsBuilder::new("default_auth_options")
            .with_auth_scheme_option_resolver(Some(StaticAuthSchemeOptionResolver::new(
                auth_schemes,
            )));
        Self { runtime_components }
    }
}

impl RuntimePlugin for DefaultAuthOptionsPlugin {
    fn order(&self) -> Order {
        Order::Defaults
    }

    fn runtime_components(
        &self,
        _current_components: &RuntimeComponentsBuilder,
    ) -> Cow<'_, RuntimeComponentsBuilder> {
        Cow::Borrowed(&self.runtime_components)
    }
}
