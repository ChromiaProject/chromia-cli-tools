package com.chromia.build.tools.config

import net.postchain.client.config.PostchainClientConfig
import org.apache.commons.configuration2.MapConfiguration

/**
 * Merges [overrides] on top of [base], returning a new [PostchainClientConfig].
 *
 * [overrides] keys follow the PostchainClientConfig property naming convention
 * (e.g. `connect.timeout`, `failover.attempts`). If [overrides] is empty, [base] is returned
 * unchanged.
 */
fun PostchainClientConfig.withOverrides(overrides: Map<String, String>): PostchainClientConfig {
    if (overrides.isEmpty()) return this

    // PostchainClientConfig.fromConfiguration() requires "brid" and "api.url" to be present,
    // so we inject them as fallbacks if the user has not explicitly overridden them.
    val fullOverrides = overrides.toMutableMap().apply {
        if (!containsKey("brid")) put("brid", blockchainRid.toHex())
        if (!containsKey("api.url")) put("api.url", endpointPool.first().url)
    }
    
    return PostchainClientConfig.fromConfiguration(MapConfiguration(fullOverrides), this)
}
