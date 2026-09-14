package com.vizx.mongodbclient.dns

import com.mongodb.spi.dns.DnsClient
import com.mongodb.spi.dns.DnsClientProvider

/**
 * Service Provider implementation for MongoDB Driver's DnsClientProvider SPI.
 * Automatically discovered by ServiceLoader to provide AndroidDnsClient.
 */
class AndroidDnsClientProvider : DnsClientProvider {
    override fun create(): DnsClient {
        return AndroidDnsClient()
    }
}
