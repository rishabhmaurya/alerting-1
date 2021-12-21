/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.alerting.core.action.node

import org.opensearch.action.support.nodes.BaseNodeResponse
import org.opensearch.alerting.core.JobSweeperMetrics
import org.opensearch.alerting.core.resthandler.RestScheduledJobStatsHandler
import org.opensearch.alerting.core.schedule.JobSchedulerMetrics
import org.opensearch.cluster.node.DiscoveryNode
import org.opensearch.common.io.stream.StreamInput
import org.opensearch.common.io.stream.StreamOutput
import org.opensearch.common.xcontent.ToXContent
import org.opensearch.common.xcontent.ToXContentFragment
import org.opensearch.common.xcontent.XContentBuilder

/**
 * Scheduled job stat that will be generated by each node.
 */
class ScheduledJobStats : BaseNodeResponse, ToXContentFragment {

    enum class ScheduleStatus(val status: String) {
        RED("red"),
        GREEN("green");

        override fun toString(): String {
            return status
        }
    }

    var status: ScheduleStatus
    var jobSweeperMetrics: JobSweeperMetrics? = null
    var jobInfos: Array<JobSchedulerMetrics>? = null

    constructor(si: StreamInput) : super(si) {
        this.status = si.readEnum(ScheduleStatus::class.java)
        this.jobSweeperMetrics = si.readOptionalWriteable { JobSweeperMetrics(it) }
        this.jobInfos = si.readOptionalArray({ sti: StreamInput -> JobSchedulerMetrics(sti) }, { size -> arrayOfNulls(size) })
    }

    constructor(
        node: DiscoveryNode,
        status: ScheduleStatus,
        jobSweeperMetrics: JobSweeperMetrics?,
        jobsInfo: Array<JobSchedulerMetrics>?
    ) : super(node) {
        this.status = status
        this.jobSweeperMetrics = jobSweeperMetrics
        this.jobInfos = jobsInfo
    }

    companion object {
        @JvmStatic
        fun readScheduledJobStatus(si: StreamInput) = ScheduledJobStats(si)
    }

    override fun writeTo(out: StreamOutput) {
        super.writeTo(out)
        out.writeEnum(status)
        out.writeOptionalWriteable(jobSweeperMetrics)
        out.writeOptionalArray(jobInfos)
    }

    override fun toXContent(builder: XContentBuilder, params: ToXContent.Params): XContentBuilder {
        builder.field("name", node.name)
        builder.field("schedule_status", status)
        builder.field("roles", node.roles.map { it.roleName().toUpperCase() })
        if (jobSweeperMetrics != null) {
            builder.startObject(RestScheduledJobStatsHandler.JOB_SCHEDULING_METRICS)
            jobSweeperMetrics!!.toXContent(builder, params)
            builder.endObject()
        }

        if (jobInfos != null) {
            builder.startObject(RestScheduledJobStatsHandler.JOBS_INFO)
            for (job in jobInfos!!) {
                builder.startObject(job.scheduledJobId)
                job.toXContent(builder, params)
                builder.endObject()
            }
            builder.endObject()
        }
        return builder
    }
}
