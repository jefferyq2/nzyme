/*
 * This file is part of nzyme.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */

package app.nzyme.core.tables.dns;

import app.nzyme.core.ethernet.EthernetRegistryKeys;
import app.nzyme.core.integrations.geoip.GeoIpLookupResult;
import app.nzyme.core.rest.resources.taps.reports.tables.dns.DnsEntropyLogReport;
import app.nzyme.core.rest.resources.taps.reports.tables.dns.DnsIpStatisticsReport;
import app.nzyme.core.rest.resources.taps.reports.tables.dns.DnsLogReport;
import app.nzyme.core.rest.resources.taps.reports.tables.dns.DnsTablesReport;
import app.nzyme.core.tables.DataTable;
import app.nzyme.core.tables.TablesService;
import app.nzyme.core.util.MetricNames;
import com.codahale.metrics.Timer;
import com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.joda.time.DateTime;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class DNSTable implements DataTable {

    private static final Logger LOG = LogManager.getLogger(DNSTable.class);

    private final TablesService tablesService;

    private final Timer totalReportTimer;
    private final Timer statisticsReportTimer;
    private final Timer pairsReportTimer;
    private final Timer logReportTimer;
    private final Timer entropyReportTimer;

    public DNSTable(TablesService tablesService) {
        this.tablesService = tablesService;

        this.totalReportTimer = tablesService.getNzyme().getMetrics()
                .timer(MetricNames.DNS_TOTAL_REPORT_PROCESSING_TIMER);
        this.statisticsReportTimer = tablesService.getNzyme().getMetrics()
                .timer(MetricNames.DNS_STATISTICS_REPORT_PROCESSING_TIMER);
        this.pairsReportTimer = tablesService.getNzyme().getMetrics()
                .timer(MetricNames.DNS_PAIRS_REPORT_PROCESSING_TIMER);
        this.logReportTimer = tablesService.getNzyme().getMetrics()
                .timer(MetricNames.DNS_LOG_REPORT_PROCESSING_TIMER);
        this.entropyReportTimer = tablesService.getNzyme().getMetrics()
                .timer(MetricNames.DNS_ENTROPY_REPORT_PROCESSING_TIMER);
    }

    public void handleReport(UUID tapUuid, DateTime timestamp, DnsTablesReport report) {
        try (Timer.Context ignored = totalReportTimer.time()) {
            tablesService.getNzyme().getDatabase().useHandle(handle -> {
                try (Timer.Context ignored2 = statisticsReportTimer.time()) {
                    registerStatistics(handle, tapUuid, report.ips(), timestamp);
                }

                try (Timer.Context ignored2 = pairsReportTimer.time()) {
                    registerPairs(handle, tapUuid, report.queryLog(), timestamp);
                }

                try (Timer.Context ignored2 = logReportTimer.time()) {
                    registerLogs(handle, tapUuid, report.queryLog(), report.responseLog());
                }

                try (Timer.Context ignored2 = entropyReportTimer.time()) {
                    registerEntropyLogs(handle, tapUuid, report.entropyLog());
                }
            });
        }
    }

    private void registerStatistics(Handle handle,
                                    UUID tapUuid,
                                    Map<String, DnsIpStatisticsReport> m,
                                    DateTime timestamp) {
        PreparedBatch batch = handle.prepareBatch("INSERT INTO dns_statistics(tap_uuid, ip, request_count, " +
                "request_bytes, response_count, response_bytes, nxdomain_count, created_at) VALUES(:tap_uuid, " +
                ":ip::inet, :request_count, :request_bytes, :response_count, :response_bytes, :nxdomain_count, " +
                ":created_at)");

        for (Map.Entry<String, DnsIpStatisticsReport> x : m.entrySet()) {
            String ip = x.getKey();
            DnsIpStatisticsReport stats = x.getValue();

            batch.bind("tap_uuid", tapUuid)
                    .bind("ip", ip)
                    .bind("request_count", stats.requestCount())
                    .bind("request_bytes", stats.requestBytes())
                    .bind("response_count", stats.responseCount())
                    .bind("response_bytes", stats.responseBytes())
                    .bind("nxdomain_count", stats.nxDomainCount())
                    .bind("created_at", timestamp)
                    .add();
        }

        batch.execute();
    }

    private void registerPairs(Handle handle, UUID tapUuid, List<DnsLogReport> logs, DateTime timestamp) {
        // Build pairs.
        Map<String, Map<Integer, Map<String, Long>>> pairs = Maps.newHashMap();
        for (DnsLogReport log : logs) {
            if (!pairs.containsKey(log.clientAddress())) {
                pairs.put(log.clientAddress(), Maps.newHashMap());
            }

            Map<Integer, Map<String, Long>> server = pairs.get(log.clientAddress());
            if (!server.containsKey(log.serverPort())) {
                server.put(log.serverPort(), Maps.newHashMap());
            }

            Map<String, Long> port = server.get(log.serverPort());
            if (!port.containsKey(log.serverAddress())) {
                port.put(log.serverAddress(), 1L);
            } else {
                port.compute(log.serverAddress(), (k, oldCount) -> oldCount + 1);
            }
        }

        PreparedBatch batch = handle.prepareBatch("INSERT INTO dns_pairs(tap_uuid, client_address, " +
                "server_address, server_port, server_address_geo_asn_number, server_address_geo_asn_name, " +
                "server_address_geo_asn_domain, server_address_geo_city, server_address_geo_country_code, " +
                "server_address_geo_latitude, server_address_geo_longitude, server_address_is_site_local, " +
                "server_address_is_multicast, server_address_is_loopback, count, " +
                "created_at) VALUES(:tap_uuid, :client_address::inet, :server_address::inet, :server_port, " +
                ":server_address_geo_asn_number, :server_address_geo_asn_name, :server_address_geo_asn_domain, " +
                ":server_address_geo_city, :server_address_geo_country_code, :server_address_geo_latitude, " +
                ":server_address_geo_longitude, :server_address_is_site_local, :server_address_is_multicast, " +
                ":server_address_is_loopback, :count, :timestamp)");

        for (Map.Entry<String, Map<Integer, Map<String, Long>>> pair : pairs.entrySet()) {
            for (Map.Entry<Integer, Map<String, Long>> server : pair.getValue().entrySet()) {
                for (Map.Entry<String, Long> port : server.getValue().entrySet()) {
                    InetAddress serverAddress;
                    try {
                        serverAddress = InetAddress.getByName(port.getKey());
                    } catch (UnknownHostException e) {
                        // This shouldn't happen because we pass IP addresses.
                        throw new RuntimeException(e);
                    }

                    Optional<GeoIpLookupResult> geo = tablesService.getNzyme().getGeoIpService().lookup(serverAddress);

                    batch.bind("tap_uuid", tapUuid)
                            .bind("client_address", pair.getKey())
                            .bind("server_address", port.getKey())
                            .bind("server_port", server.getKey())
                            .bind("server_address_geo_asn_number", geo.map(g -> g.asn().number()).orElse(null))
                            .bind("server_address_geo_asn_name", geo.map(g -> g.asn().name()).orElse(null))
                            .bind("server_address_geo_asn_domain", geo.map(g -> g.asn().domain()).orElse(null))
                            .bind("server_address_geo_city", geo.map(g -> g.geo().city()).orElse(null))
                            .bind("server_address_geo_country_code", geo.map(g -> g.geo().countryCode()).orElse(null))
                            .bind("server_address_geo_latitude", geo.map(g -> g.geo().latitude()).orElse(null))
                            .bind("server_address_geo_longitude", geo.map(g -> g.geo().longitude()).orElse(null))
                            .bind("server_address_is_site_local", serverAddress.isSiteLocalAddress())
                            .bind("server_address_is_multicast", serverAddress.isMulticastAddress())
                            .bind("server_address_is_loopback", serverAddress.isLoopbackAddress())
                            .bind("count", port.getValue())
                            .bind("timestamp", timestamp)
                            .add();
                }
            }
        }

        batch.execute();
    }

    private void registerLogs(Handle handle,
                              UUID tapUuid,
                              List<DnsLogReport> queries,
                              List<DnsLogReport> responses) {
        PreparedBatch batch = handle.prepareBatch("INSERT INTO dns_log(uuid, tap_uuid, transaction_id, " +
                "dns_type, client_address, client_port, client_mac, server_address, server_port, server_mac, " +
                "data_value, data_value_etld, data_type,  timestamp, created_at) VALUES(:uuid, :tap_uuid, " +
                ":transaction_id, :dns_type, :client_address::inet, :client_port, :client_mac, " +
                ":server_address::inet, :server_port, :server_mac, :data_value, :data_value_etld, :data_type, " +
                ":timestamp, NOW())");

        for (DnsLogReport d : queries) {
            batch
                    .bind("uuid", UUID.randomUUID())
                    .bind("tap_uuid", tapUuid)
                    .bind("transaction_id", d.transactionId())
                    .bind("dns_type", "query")
                    .bind("client_address", d.clientAddress())
                    .bind("client_port", d.clientPort())
                    .bind("client_mac", d.clientMac())
                    .bind("server_address", d.serverAddress())
                    .bind("server_port", d.serverPort())
                    .bind("server_mac", d.serverMac())
                    .bind("data_value", d.dataValue())
                    .bind("data_value_etld", d.dataValueEtld())
                    .bind("data_type", d.dataType())
                    .bind("timestamp", d.timestamp())
                    .add();
        }

        for (DnsLogReport d : responses) {
            batch
                    .bind("uuid", UUID.randomUUID())
                    .bind("tap_uuid", tapUuid)
                    .bind("transaction_id", d.transactionId())
                    .bind("dns_type", "response")
                    .bind("client_address", d.clientAddress())
                    .bind("client_port", d.clientPort())
                    .bind("client_mac", d.clientMac())
                    .bind("server_address", d.serverAddress())
                    .bind("server_port", d.serverPort())
                    .bind("server_mac", d.serverMac())
                    .bind("data_value", d.dataValue())
                    .bind("data_value_etld", d.dataValueEtld())
                    .bind("data_type", d.dataType())
                    .bind("timestamp", d.timestamp())
                    .add();
        }

        batch.execute();
    }

    public void registerEntropyLogs(Handle handle,
                                    UUID tapUuid,
                                    List<DnsEntropyLogReport> logs) {
        PreparedBatch batch = handle.prepareBatch("INSERT INTO dns_entropy_log(tap_uuid, transaction_id, " +
                "entropy, entropy_mean, zscore, timestamp, created_at) VALUES(:tap_uuid, :transaction_id, " +
                ":entropy, :entropy_mean, :zscore, :timestamp, NOW())");

        for (DnsEntropyLogReport log : logs) {
            batch
                    .bind("tap_uuid", tapUuid)
                    .bind("transaction_id", log.transactionId())
                    .bind("entropy", log.entropy())
                    .bind("entropy_mean", log.entropyMean())
                    .bind("zscore", log.zScore())
                    .bind("timestamp", log.timestamp())
                    .add();
        }

        batch.execute();
    }

    @Override
    public void retentionClean() {
        int retentionTimeDays = Integer.parseInt(tablesService.getNzyme().getDatabaseCoreRegistry()
                .getValue(EthernetRegistryKeys.DNS_RETENTION_TIME_DAYS.key())
                .orElse(EthernetRegistryKeys.DNS_RETENTION_TIME_DAYS.defaultValue().orElse("MISSING"))
        );

        tablesService.getNzyme().getDatabase().useHandle(handle -> {
            handle.createUpdate("DELETE FROM dns_statistics WHERE created_at < :created_at")
                    .bind("created_at", DateTime.now().minusDays(retentionTimeDays))
                    .execute();

            handle.createUpdate("DELETE FROM dns_pairs WHERE created_at < :created_at")
                    .bind("created_at", DateTime.now().minusDays(retentionTimeDays))
                    .execute();

            handle.createUpdate("DELETE FROM dns_log WHERE timestamp < :created_at")
                    .bind("created_at", DateTime.now().minusDays(retentionTimeDays))
                    .execute();

            handle.createUpdate("DELETE FROM dns_entropy_log WHERE timestamp < :created_at")
                    .bind("created_at", DateTime.now().minusDays(retentionTimeDays))
                    .execute();
        });
    }
}
