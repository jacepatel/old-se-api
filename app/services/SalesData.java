package services;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import play.db.jpa.JPA;

import javax.persistence.Query;
import javax.persistence.TemporalType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * Created by michaelsive on 29/07/15.
 */
public class SalesData {

    @JsonIgnore
    private Long truckId;

    @JsonIgnore
    private Date fromDate;

    @JsonIgnore
    private Date toDate;

    public SalesData(Long truckId, Date fromDate, Date toDate){
        this.truckId = truckId;
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    // Find total value and order count between dates.
    public HashMap totalValuesAndOrderCounts(){
        Query totalValueQuery = JPA.em().createNativeQuery("select orders.ordertype, COALESCE(sum(orders.orderTotalPaid),0), count(orders.orderId) from orders inner join trucksessions on orders.trucksessionid = trucksessions.trucksessionid where orders.orderstatus = 4 and trucksessions.starttime between ? and ? and trucksessions.truckid = ? group by orders.ordertype");
        totalValueQuery.setParameter(1, this.fromDate, TemporalType.DATE);
        totalValueQuery.setParameter(2, this.toDate, TemporalType.DATE);
        totalValueQuery.setParameter(3, this.truckId);
        List<Object[]> totalValueAndOrderCount = totalValueQuery.getResultList();
        HashMap valueAndCountsByType = new HashMap();
        for (Object[] row : totalValueAndOrderCount){
            int orderType = (int)row[0];
            HashMap type = new HashMap();
            type.put("total", row[1]);
            type.put("count", row[2]);
            if (orderType == 1){
                valueAndCountsByType.put("mobile", type);
            }
            else if (orderType == 2){
                valueAndCountsByType.put("cash", type);
            }
            else {
                valueAndCountsByType.put("eftpos", type);
            }

        }
        return valueAndCountsByType;
    }

    // Get a count for each item from orders in date range
    public HashMap itemCounts(){
        Query itemCountsQuery = JPA.em().createNativeQuery("select items.name, sum(orderitems.quantity) from items inner join orderitems on items.itemId = orderitems.itemId where orderitems.orderId in (select orders.orderId from orders inner join trucksessions on trucksessions.truckSessionId = orders.truckSessionId where trucksessions.startTime between ? and ? and orders.orderStatus = 4 and orders.truckId = ?) group by items.name");
        itemCountsQuery.setParameter(1, this.fromDate, TemporalType.DATE);
        itemCountsQuery.setParameter(2, this.toDate, TemporalType.DATE);
        itemCountsQuery.setParameter(3, this.truckId);
        List<Object[]> itemCountsResultSet = itemCountsQuery.getResultList();
        HashMap itemCounts = new HashMap();
        for (Object[] row : itemCountsResultSet){
            itemCounts.put(row[0], row[1]);
        }
        return itemCounts;
    }

    // Get a list of days and order count for each day
     public HashMap getValueByDays(){
         Query totalValueForDaysQuery = JPA.em().createNativeQuery("SELECT to_char(TS.StartTime + TZ.utc_offset, 'dd/mm/yy'), COUNT(*) AS OrderCount, SUM(OrderTotalPaid) AS OrderTotalPaid " +
           "FROM Orders O INNER JOIN TruckSessions TS ON TS.TruckSessionID = O.TruckSessionId " +
           "INNER JOIN Trucks T ON T.TruckId = TS.TruckId " +
           "INNER JOIN pg_timezone_names TZ ON T.timezoneName = TZ.name " +
           "WHERE O.TruckId = ? AND O.OrderStatus = 4 AND TS.StartTime BETWEEN ? AND ? GROUP BY to_char(TS.StartTime + TZ.utc_offset, 'dd/mm/yy'), to_char(TS.StartTime + TZ.utc_offset, 'MM'), to_char(TS.StartTime + TZ.utc_offset, 'YY'), to_char(TS.StartTime + TZ.utc_offset, 'DD') ORDER BY to_char(TS.StartTime + TZ.utc_offset, 'YY') ASC, to_char(TS.StartTime + TZ.utc_offset, 'MM') ASC, to_char(TS.StartTime + TZ.utc_offset, 'DD')  ASC");
         totalValueForDaysQuery.setParameter(1, this.truckId);
         totalValueForDaysQuery.setParameter(2, this.fromDate, TemporalType.DATE);
         totalValueForDaysQuery.setParameter(3, this.toDate, TemporalType.DATE);
         List<Object[]> dayCountsResultSet = totalValueForDaysQuery.getResultList();
         HashMap dayValues = new HashMap();
         int count = dayCountsResultSet.size();
         for (int i = 0; i < count; i++){
             HashMap dataSet = new HashMap();
             Object[] row = dayCountsResultSet.get(i);
             dataSet.put(row[0], row[2]);
             dayValues.put(i, dataSet);
         }
         return dayValues;
     }

    // Get session data
    public List truckSessionData(){
        Query truckSessionsQuery = JPA.em().createNativeQuery("SELECT TS.StartTime, TS.EndTime, TS.Address, COUNT(*) AS OrderCount, SUM(OrderTotalPaid) AS OrderTotalValue, SUM(CASE WHEN OrderType = 1 THEN OrderTotalPaid ELSE 0 END) AS MobileTotalValue, SUM(CASE WHEN OrderType = 2 THEN OrderTotalPaid ELSE 0 END) AS CashTotalValue, SUM(CASE WHEN OrderType = 3 THEN OrderTotalPaid ELSE 0 END) AS EftposTotalValue, SUM(OrderTotalPaid) / COUNT(*) AS AverageOrderValue, AVG(EXTRACT(EPOCH FROM ReadyTime-orderTime)/60) AS AverageOrderTime, TS.TruckSessionID FROM Orders O INNER JOIN TruckSessions TS ON TS.TruckSessionID = O.TruckSessionId INNER JOIN Trucks T ON T.TruckId = TS.Truckid WHERE T.TruckId = ? AND TS.StartTime BETWEEN ? AND ? and O.orderStatus = 4 GROUP BY TS.StartTime, TS.EndTime, TS.Address, TS.TruckSessionID ORDER BY TS.StartTime DESC");
        truckSessionsQuery.setParameter(1, this.truckId);
        truckSessionsQuery.setParameter(2, this.fromDate, TemporalType.DATE);
        truckSessionsQuery.setParameter(3, this.toDate, TemporalType.DATE);
        List<Object[]> truckSessionData = truckSessionsQuery.getResultList();
        ArrayList<HashMap> truckSessions = new ArrayList<>();
        for (Object[] row : truckSessionData){
            HashMap ts = new HashMap();
            ts.put("startTime", row[0]);
            ts.put("endTime", row[1]);
            ts.put("address", row[2]);
            ts.put("orderCount", row[3]);
            ts.put("total", row[4]);
            ts.put("mobileTotal", row[5]);
            ts.put("cashTotal", row[6]);
            ts.put("eftposTotal", row[7]);
            ts.put("averageValue", row[8]);
            ts.put("averageTime", row[9]);
            ts.put("truckSessionId", row[10]);
            truckSessions.add(ts);
        }
        return truckSessions;
    }

    public HashMap compiledData(){
        HashMap salesData = new HashMap();
        HashMap totalValueAndOrderCount = this.totalValuesAndOrderCounts();
        salesData.put("totals", totalValueAndOrderCount);
        salesData.put("items", this.itemCounts());
        salesData.put("days", this.getValueByDays());
        salesData.put("truckSessions", this.truckSessionData());
        return salesData;
    }

}
