package services;

import controllers.Application;
import models.Order;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import play.db.jpa.JPA;

import javax.persistence.Query;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by michaelsive on 3/08/15.
 */
public class OrdersReport {
    private Date fromDate;
    private Date toDate;
    private String path;
    private DateFormat dateFormat;
    private DateFormat durationFormat;
    private Workbook wb;
    private Sheet summarySheet;
    private int rowCount;
    private Font bold;
    private CellStyle head;
    private CellStyle money;
    private FileOutputStream out;
    private Long truckId;
    private final int mobile = 1;
    private final int cash = 2;
    private final int eftpos = 3;


    public OrdersReport(Long truckId, Date fromDate, Date toDate) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.truckId = truckId;

        rowCount = 0;
        int summarySheetIndex = 0;

        //Set date formatters
        dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("Australia/Brisbane"));
        durationFormat = new SimpleDateFormat("mm:ss");
        durationFormat.setTimeZone(TimeZone.getTimeZone("Australia/Brisbane"));

        //Set up file
        File tmp = new File("/tmp");
        tmp.mkdirs();
        this.path = "/tmp/Orders" + "-from-" + dateFormat.format(fromDate) + "-to-" + dateFormat.format(toDate) + ".xls";

        // Create a new workbook
        wb = new HSSFWorkbook();

        // Setup Orders sheet
        summarySheet = wb.createSheet();
        wb.setSheetName(0, "Orders");

        // Setup styles, header
        setUpStyles();
        setUpHeader();

        // Get order data
        addOrdersToSheet(getOrders());

    }

    private void setUpStyles(){
        head = wb.createCellStyle();
        money = wb.createCellStyle();
        money.setDataFormat((short) 7);
        money.setAlignment(CellStyle.ALIGN_RIGHT);

        // Setup fonts and styles
        bold = wb.createFont();
        bold.setBoldweight(Font.BOLDWEIGHT_BOLD);//Make font bold
        head.setFont(bold);
    }

    private void setUpHeader(){
        // Set up header
        Row headerOne = newRow();
        Cell ordersText = newCell(headerOne);
        ordersText.setCellValue("Orders");
        ordersText.setCellStyle(head);

        Row headerTwo = newRow();

        // Set up from date
        Cell fromText = newCell(headerTwo);
        fromText.setCellValue("From Date");
        fromText.setCellStyle(head);
        Cell fromValue = newCell(headerTwo);
        fromValue.setCellValue(dateFormat.format(fromDate));

        // Set up to date
        Cell toText = newCell(headerTwo);
        toText.setCellValue("To Date");
        toText.setCellStyle(head);
        Cell toValue = newCell(headerTwo);
        toValue.setCellValue(dateFormat.format(toDate));

        Row columnHeaders = newRow();
        String[] columnNames = {"Order Id", "Order Type", "Order Name", "Order Duration (Minutes)", "Comments", "Discount", "Total Paid", "Processing Fee", "SMS Fee"};
        for (String headerName : columnNames){
            Cell columnHead = newCell(columnHeaders);
            columnHead.setCellValue(headerName);
            columnHead.setCellStyle(head);
        }
    }

    private List<Order> getOrders(){
        Query query = JPA.em().createQuery("Select o from Order o " +
          "inner join o.truckSession ts " +
          " where ts.truckId = :truckId and ts.startTime between :fromDate and :toDate and o.orderStatus = 4");
        query.setParameter("truckId", this.truckId);
        query.setParameter("fromDate", this.fromDate);
        query.setParameter("toDate", this.toDate);
        List<Order> orders = query.getResultList();
        return orders;
    }

    private void addOrdersToSheet(List<Order> orders){
        for (Order o : orders){
            Row orderRow = newRow();
            Cell orderId = newCell(orderRow);
            orderId.setCellValue(o.orderId);

            Cell orderType = newCell(orderRow);
            switch(o.orderType){
                case mobile: orderType.setCellValue("Mobile");
                    break;
                case cash: orderType.setCellValue("Cash");
                    break;
                case eftpos: orderType.setCellValue("Eftpos");
            }

            Cell orderName = newCell(orderRow);
            orderName.setCellValue(o.orderName);

            Cell orderDuration = newCell(orderRow);
            Long difference = (o.readyTime.getTime() - o.orderTime.getTime());
            Date diff = new Date(difference);
            orderDuration.setCellValue(durationFormat.format(diff));

            Cell orderComments = newCell(orderRow);
            orderComments.setCellValue(o.comments);

            Cell orderDiscount = newCell(orderRow);
            if (o.discount != null) {
                orderDiscount.setCellValue(o.discount.floatValue());
                orderDiscount.setCellStyle(money);
            }
            Cell orderTotalPaid = newCell(orderRow);
            orderTotalPaid.setCellValue(o.orderTotal.floatValue());
            orderTotalPaid.setCellStyle(money);
            Cell procFee = newCell(orderRow);
            if (o.orderType == mobile && (o.orderStatus == Order.STATUS_COMPLETED || o.orderStatus == Order.STATUS_READY_TO_COLLECT || o.orderStatus == Order.STATUS_CONFIRMED)){
                BigDecimal cut = new BigDecimal(0.035).setScale(3, RoundingMode.DOWN);
                BigDecimal fee = cut.multiply(o.orderTotal).setScale(2, RoundingMode.DOWN);
                fee = fee.add(new BigDecimal(0.17).setScale(2, RoundingMode.DOWN));
                procFee.setCellValue(fee.floatValue());
                procFee.setCellStyle(money);
            }
            Cell smsFee = newCell(orderRow);
            if (o.orderType != mobile && !o.mobileNumber.isEmpty() && o.mobileNumber != null){
                smsFee.setCellValue(0.05);
                smsFee.setCellStyle(money);
            }
        }
    }

    private void incrementRowCount(){
        rowCount += 1;
    }

    private Row newRow(){
        Row newRow =  summarySheet.createRow(rowCount);
        incrementRowCount();
        return newRow;
    }

    private Cell newCell(Row row){
        int lastCellNum = row.getLastCellNum();
        if (lastCellNum < 0){
            return row.createCell(0);
        }
        return row.createCell(row.getLastCellNum());
    }

    public String generateFile() throws IOException {
        out = new FileOutputStream(this.path);
        int columnCountSummary = 9;
        for (int i = 0; i < columnCountSummary; i++){
            summarySheet.autoSizeColumn(i);
        }
        wb.write(out);
        out.flush();
        out.close();
        return this.path;
    }
}
