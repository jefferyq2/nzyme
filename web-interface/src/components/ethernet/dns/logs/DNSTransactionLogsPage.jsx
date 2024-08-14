import React, {useContext, useEffect, useState} from 'react';
import ApiRoutes from "../../../../util/ApiRoutes";
import CardTitleWithControls from "../../../shared/CardTitleWithControls";
import {Presets} from "../../../shared/timerange/TimeRange";
import DNSTransactionsTable from "./DNSTransactionsTable";
import {TapContext} from "../../../../App";
import {disableTapSelector, enableTapSelector} from "../../../misc/TapSelector";
import Filters from "../../../shared/filtering/Filters";
import DNSTransactionCountChart from "./widgets/DNSTransactionCountChart";
import {DNS_FILTER_FIELDS} from "../DNSFilterFields";
import {useLocation} from "react-router-dom";
import AlphaFeatureAlert from "../../../shared/AlphaFeatureAlert";
import HeadlineMenu from "../../../shared/HeadlineMenu";
import {DNS_HEADLINE_MENU_ITEMS} from "../DNSHeadlineMenuItems";

const useQuery = () => {
  return new URLSearchParams(useLocation().search);
}

export default function DNSTransactionLogsPage() {

  const tapContext = useContext(TapContext);
  const urlQuery = useQuery();

  let filtersJson;
  try {
    if (urlQuery.get("filters")) {
      filtersJson = JSON.parse(urlQuery.get("filters"));
    } else {
      filtersJson = null;
    }
  } catch (error) {
    console.error("Failed to parse filter URL parameter JSON.");
    filtersJson = null;
  }

  const [timeRange, setTimeRange] = useState(Presets.RELATIVE_HOURS_24);
  const [filters, setFilters] = useState(filtersJson);
  const [revision, setRevision] = useState(new Date());

  useEffect(() => {
    enableTapSelector(tapContext);

    return () => {
      disableTapSelector(tapContext);
    }
  }, [tapContext]);

  return (
      <React.Fragment>
        <AlphaFeatureAlert/>

        <div className="row">
          <div className="col-12">
            <nav aria-label="breadcrumb">
              <ol className="breadcrumb">
                <li className="breadcrumb-item"><a href={ApiRoutes.ETHERNET.OVERVIEW}>Ethernet</a></li>
                <li className="breadcrumb-item"><a href={ApiRoutes.ETHERNET.DNS.INDEX}>DNS</a></li>
                <li className="breadcrumb-item active" aria-current="page">Transaction Log</li>
              </ol>
            </nav>
          </div>

          <div className="row mb-3">
            <div className="col-md-12">
              <HeadlineMenu headline={"DNS Transaction Logs"}
                            items={DNS_HEADLINE_MENU_ITEMS}
                            activeRoute={ApiRoutes.ETHERNET.DNS.TRANSACTION_LOGS}/>
            </div>
          </div>

          <div className="col-12">
            <div className="card">
              <div className="card-body">
                <CardTitleWithControls title="Filters"
                                       hideTimeRange={true}
                                       timeRange={timeRange}
                                       setTimeRange={setTimeRange}
                                       slim={true}/>

                <Filters filters={filters} setFilters={setFilters} fields={DNS_FILTER_FIELDS} />
              </div>
            </div>
          </div>
        </div>

        <div className="row mt-3">
          <div className="col-12">
            <div className="card">
              <div className="card-body">
                <CardTitleWithControls title="Transaction Count"
                                       fixedAppliedTimeRange={timeRange}
                                       refreshAction={() => { setRevision(new Date()) }} />

                <DNSTransactionCountChart timeRange={timeRange} filters={filters} setFilters={setFilters} revision={revision} />
              </div>
            </div>
          </div>
        </div>

        <div className="row mt-3">
          <div className="col-12">
            <div className="card">
              <div className="card-body">
                <CardTitleWithControls title="Transactions"
                                       fixedAppliedTimeRange={timeRange}
                                       refreshAction={() => { setRevision(new Date()) }} />

                <DNSTransactionsTable timeRange={timeRange} filters={filters} setFilters={setFilters} revision={revision}/>
              </div>
            </div>
          </div>
        </div>
      </React.Fragment>
  )

}