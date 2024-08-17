import React, {useState} from "react";
import moment from "moment/moment";

import numeral from "numeral";
import DNSData from "../DNSData";
import ETLD from "../../shared/ETLD";
import L4Address from "../../shared/L4Address";
import DNSLogResponseTable from "../shared/DNSLogResponseTable";

export default function DNSEntropyLogTableRow(props) {

  const log = props.log;
  const [showResponses, setShowResponses] = useState(false);

  const toggleVisibility = (e) => {
    e.preventDefault();

    setShowResponses(!showResponses);
  }

  return (
      <React.Fragment>
        <tr>
          <td>
            <DNSData value={log.query.data_value}/>
            &nbsp;&nbsp;

            <a href="#" title="Show Responses" onClick={toggleVisibility}>
              <i className="fa-solid fa-chevron-down"></i>
            </a>
          </td>
          <td>{log.query.data_type}</td>
          <td title={log.query.timestamp}>{moment(log.query.timestamp).format()}</td>
          <td title={log.query.data_value_etld}><ETLD etld={log.query.data_value_etld}/></td>
          <td><L4Address address={log.query.client} hidePort={true} /></td>
          <td><L4Address address={log.query.server} /></td>
          <td title={log.entropy + " / " + log.entropy_mean}>
            {numeral(log.entropy).format("0,0.00")} / {numeral(log.entropy_mean).format("0,0.00")}
          </td>
          <td title={log.zscore}>{numeral(log.zscore).format("0,0.0")}</td>
        </tr>

        <DNSLogResponseTable transactionId={log.query.transaction_id}
                             transactionTimestamp={log.query.timestamp}
                             show={showResponses}/>
      </React.Fragment>
  )

}