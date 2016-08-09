package services;

import models.Item;
import models.Order;
import models.OrderItem;
import models.TruckSession;
import org.apache.commons.mail.EmailException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.functions.Column;
import org.apache.poi.ss.usermodel.*;
import play.Play;
import play.db.jpa.JPA;
import play.libs.F;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by michaelsive on 24/05/15.
 */
public class Report {
    private FileOutputStream out;
    private String root;
    private Workbook wb;
    private Sheet summarySheet;
    private Sheet allOrdersSheet;
    private Sheet allItemsSheet;
    private TruckSession reportSession;
    private CellStyle head;
    private CellStyle content;
    private CellStyle money;
    private CellStyle boldMoney;
    private Font bold;
    private Font regular;
    private int mobile = 1;
    private int cash = 2;
    private int eftpos = 3;
    private String path;
    private String truckName;
    private DateFormat timeFormat;
    private DateFormat durationFormat;
    private DateFormat dateFormat;
    private String sendAddress;

    public Report(TruckSession truckSession) {
        this.root = Play.application().path().getAbsolutePath();
        // Set report session
        reportSession = truckSession;

        // Set up instance variables
        this.truckName = reportSession.truck.name;
        this.sendAddress = reportSession.truck.contactEmail;
        File tmp = new File("/tmp");
        tmp.mkdirs();
        this.path = "/tmp/Report" + this.truckName + reportSession.truckSessionId + ".xls";

        // Set up date formatters
        timeFormat = new SimpleDateFormat("HH:mm:ss");
        durationFormat = new SimpleDateFormat("mm:ss");
        dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        dateFormat.setTimeZone(TimeZone.getTimeZone("Australia/Brisbane"));
        timeFormat.setTimeZone(TimeZone.getTimeZone("Australia/Brisbane"));
        durationFormat.setTimeZone(TimeZone.getTimeZone("Australia/Brisbane"));



        // Create a new workbook
        wb = new HSSFWorkbook();

        // Setup Summary sheet
        summarySheet = wb.createSheet();
        int summarySheetIndex = 0;
        wb.setSheetName(summarySheetIndex, "Summary");

        // Setup Orders Sheet
        allOrdersSheet = wb.createSheet();
        int ordersSheetIndex = 1;
        wb.setSheetName(ordersSheetIndex, "All Orders");

        // Setup Items Sheet
        allItemsSheet = wb.createSheet();
        int itemsSheetIndex = 2;
        wb.setSheetName(itemsSheetIndex, "All Order Items");

        // Setup styles
        head = wb.createCellStyle();
        content = wb.createCellStyle();
        boldMoney = wb.createCellStyle();
        money = wb.createCellStyle();
        money.setDataFormat((short)7);
        money.setAlignment(CellStyle.ALIGN_RIGHT);
        boldMoney.setDataFormat((short)7);
        boldMoney.setAlignment(CellStyle.ALIGN_RIGHT);

        // Setup fonts
        bold = wb.createFont();
        regular = wb.createFont();

        bold.setBoldweight(Font.BOLDWEIGHT_BOLD);//Make font bold
        regular.setBoldweight(Font.BOLDWEIGHT_NORMAL);

        head.setFont(bold);
        boldMoney.setFont(bold);
        content.setFont(regular);
    }

    private void prepareSummarySheet(){
        short rowNumber = 0;
        //Create Header Section
        Row header = summarySheet.createRow(rowNumber);
        Cell truckName = header.createCell(0);
        Cell sessionDate = header.createCell(1);
        truckName.setCellValue(reportSession.truck.name);
        sessionDate.setCellValue(dateFormat.format(reportSession.startTime));
        truckName.setCellStyle(head);
        sessionDate.setCellStyle(head);
        Cell addressHead = header.createCell(3);
        addressHead.setCellValue("Address");
        addressHead.setCellStyle(head);
        Cell address = header.createCell(4);
        address.setCellValue(reportSession.address);
        rowNumber += 1;

        // Create From Section
        Row from = summarySheet.createRow(rowNumber);
        Cell fromText = from.createCell(0);
        Cell fromTime = from.createCell(1);
        fromText.setCellValue("From");
        fromTime.setCellValue(timeFormat.format(reportSession.startTime));

        //Create tsId Section
        Cell tsIdHead = from.createCell(3);
        tsIdHead.setCellValue("Session Id");
        tsIdHead.setCellStyle(head);
        Cell tsId = from.createCell(4);
        tsId.setCellValue(reportSession.truckSessionId);

        rowNumber += 1;

        // Create To Section
        Row to = summarySheet.createRow(rowNumber);
        Cell toText = to.createCell(0);
        Cell toTime = to.createCell(1);
        toText.setCellValue("To");
        toTime.setCellValue(timeFormat.format(reportSession.endTime));
        rowNumber += 2;

        // Create Cash/Mobile/Eftpos Section
        Row cashMobileHeader = summarySheet.createRow(rowNumber);
        Cell orderTypeText = cashMobileHeader.createCell(0);
        orderTypeText.setCellValue("OrderType");
        Cell totalValueText = cashMobileHeader.createCell(1);
        totalValueText.setCellValue("Total Value");
        Cell procFeesText = cashMobileHeader.createCell(2);
        procFeesText.setCellValue("Processing Fees");
        Cell smsFeesText = cashMobileHeader.createCell(3);
        smsFeesText.setCellValue("SMS Fees");
        Cell avgTimeText = cashMobileHeader.createCell(4);
        avgTimeText.setCellValue("Average Order Time (Minutes)");
        Cell orderCountText = cashMobileHeader.createCell(5);
        orderCountText.setCellValue("Order Count");
        setStyleOfRow(cashMobileHeader, head);
        rowNumber += 1;

        // Set mobile values
        Row mobileRow = summarySheet.createRow(rowNumber);
        Cell mobileType = mobileRow.createCell(0);
        mobileType.setCellValue("Mobile");
        Cell mobileValue = mobileRow.createCell(1);
        mobileValue.setCellValue(getTotalValueOfType(mobile).floatValue());
        mobileValue.setCellStyle(money);
        Cell procFees = mobileRow.createCell(2);
        procFees.setCellValue(getProcessingFees().floatValue());
        procFees.setCellStyle(money);
        Cell averageOrderTimeMobile = mobileRow.createCell(4);
        averageOrderTimeMobile.setCellValue(getAverageOrderTime(mobile));
        Cell orderCountMobile = mobileRow.createCell(5);
        orderCountMobile.setCellValue(reportSession.orderCountOfType(mobile));
        rowNumber += 1;

        // Set cash values
        Row cashRow = summarySheet.createRow(rowNumber);
        Cell cashType = cashRow.createCell(0);
        cashType.setCellValue("Cash");
        Cell cashValue = cashRow.createCell(1);
        cashValue.setCellValue(getTotalValueOfType(cash).floatValue());
        cashValue.setCellStyle(money);
        Cell smsFees = cashRow.createCell(3);
        smsFees.setCellValue(getSMSFees().floatValue());
        smsFees.setCellStyle(money);
        Cell averageOrderTimeCash = cashRow.createCell(4);
        averageOrderTimeCash.setCellValue(getAverageOrderTime(cash));
        Cell orderCountCash = cashRow.createCell(5);
        orderCountCash.setCellValue(reportSession.orderCountOfType(cash));
        rowNumber += 1;

        // Set Efpos values
        Row eftposRow = summarySheet.createRow(rowNumber);
        Cell eftposType = eftposRow.createCell(0);
        eftposType.setCellValue("Eftpos");
        Cell eftposValue = eftposRow.createCell(1);
        eftposValue.setCellValue(getTotalValueOfType(eftpos).floatValue());
        eftposValue.setCellStyle(money);
        Cell eftsmsFees = eftposRow.createCell(3);
        eftsmsFees.setCellValue(0);
        eftsmsFees.setCellStyle(money);
        Cell averageOrderTimeEftpos = eftposRow.createCell(4);
        averageOrderTimeEftpos.setCellValue(getAverageOrderTime(eftpos));
        Cell orderCountEftpos = eftposRow.createCell(5);
        orderCountEftpos.setCellValue(reportSession.orderCountOfType(eftpos));
        rowNumber += 1;

        // Set total values
        Row totalRow = summarySheet.createRow(rowNumber);
        Cell totalText = totalRow.createCell(0);
        totalText.setCellValue("TOTAL");
        Cell totalValue = totalRow.createCell(1);
        totalValue.setCellValue(getTotalValueOfType(cash).add(getTotalValueOfType(mobile)).add(getTotalValueOfType(eftpos)).floatValue());
        Cell totalProcFees = totalRow.createCell(2);
        totalProcFees.setCellValue(getProcessingFees().floatValue());
        totalProcFees.setCellStyle(money);
        Cell totalSMSFees = totalRow.createCell(3);
        totalSMSFees.setCellValue(getSMSFees().floatValue());
        totalSMSFees.setCellStyle(money);
        totalRow.createCell(4);
        Cell totalOrders = totalRow.createCell(5);
        totalOrders.setCellValue(reportSession.orderCountOfType(cash) + reportSession.orderCountOfType(mobile) + reportSession.orderCountOfType(eftpos));
        setStyleOfRow(totalRow, head);
        totalValue.setCellStyle(boldMoney);
        totalProcFees.setCellStyle(boldMoney);
        totalSMSFees.setCellStyle(boldMoney);

        rowNumber += 2;

        // Set up remmited
        Row remittedRow = summarySheet.createRow(rowNumber);
        Cell remittedCell = remittedRow.createCell(0);
        remittedCell.setCellValue("Amount to be remitted");
        Cell remittedValue = remittedRow.createCell(1);
        BigDecimal remittedAmnt = getTotalValueOfType(mobile).subtract(getProcessingFees().add(getSMSFees()));
        remittedValue.setCellValue(remittedAmnt.floatValue());
        remittedValue.setCellStyle(money);
        rowNumber += 2;

        // Set up item head rows
        Row itemHead = summarySheet.createRow(rowNumber);
        Cell nameText = itemHead.createCell(0);
        nameText.setCellValue("Item Name");
        Cell qtyText = itemHead.createCell(1);
        qtyText.setCellValue("Quantity");
        Cell paidText = itemHead.createCell(2);
        paidText.setCellValue("Total Paid");
        setStyleOfRow(itemHead, head);
        rowNumber += 1;

        // Set up item rows
        List<Long> orderItemIds = reportSession.distinctItemIds();
        Long itemQtys = new Long(0);
        BigDecimal totalPaid = new BigDecimal(0).setScale(2, RoundingMode.DOWN);
        for (Long itemId : orderItemIds){
            Item item = JPA.em().find(Item.class, itemId);
            Row itemRow = summarySheet.createRow(rowNumber);
            Cell itemName = itemRow.createCell(0);
            itemName.setCellValue(item.name);
            List<String> countAndPaid = reportSession.itemTypeCountandPaid(itemId);
            itemQtys += Long.parseLong(countAndPaid.get(0));
            totalPaid = totalPaid.add(new BigDecimal(countAndPaid.get(1)));
            Cell itemQty = itemRow.createCell(1);
            Long count = Long.parseLong(countAndPaid.get(0));
            itemQty.setCellValue(count);
            Cell itemPaid = itemRow.createCell(2);
            Float paid = Float.parseFloat(countAndPaid.get(1));
            itemPaid.setCellValue(paid);
            itemPaid.setCellStyle(money);
            rowNumber += 1;
        }
        // Set up item total row
        Row itemTotal = summarySheet.createRow(rowNumber);
        Cell itemTotalText = itemTotal.createCell(0);
        itemTotalText.setCellValue("TOTAL");
        Cell itemQtyTotal = itemTotal.createCell(1);
        itemQtyTotal.setCellValue(itemQtys);
        Cell itemPaidTotal = itemTotal.createCell(2);
        itemPaidTotal.setCellValue(totalPaid.floatValue());
        itemPaidTotal.setCellStyle(money);
        setStyleOfRow(itemTotal, head);
        itemPaidTotal.setCellStyle(boldMoney);
    }

    private void prepareAllOrders(){
        short rowNum = 0;

        // Set up head
        Row allOrdersHead = allOrdersSheet.createRow(rowNum);
        Cell orderIdHead = allOrdersHead.createCell(0);
        orderIdHead.setCellValue("Order Id");
        Cell orderTypeHead = allOrdersHead.createCell(1);
        orderTypeHead.setCellValue("Order Type");
        Cell orderNameHead = allOrdersHead.createCell(2);
        orderNameHead.setCellValue("Order Name");
        Cell orderStatusHead = allOrdersHead.createCell(3);
        orderStatusHead.setCellValue("Order Status");
        Cell orderTimeHead = allOrdersHead.createCell(4);
        orderTimeHead.setCellValue("Order Time");
        Cell acceptTimeHead = allOrdersHead.createCell(5);
        acceptTimeHead.setCellValue("Accepted Time");
        Cell readyTimeHead = allOrdersHead.createCell(6);
        readyTimeHead.setCellValue("Ready Time");
        Cell collectTimeHead = allOrdersHead.createCell(7);
        collectTimeHead.setCellValue("Collect Time");
        Cell orderDurationHead = allOrdersHead.createCell(8);
        orderDurationHead.setCellValue("Order Duration (Minutes)");
        Cell commentsHead = allOrdersHead.createCell(9);
        commentsHead.setCellValue("Comments");
        Cell discountHead = allOrdersHead.createCell(10);
        discountHead.setCellValue("Discount");
        Cell totalHead = allOrdersHead.createCell(11);
        totalHead.setCellValue("Total Paid");
        Cell procfeeHead = allOrdersHead.createCell(12);
        procfeeHead.setCellValue("Processing Fee");
        Cell smsfeeHead = allOrdersHead.createCell(13);
        smsfeeHead.setCellValue("SMS Fee");
        setStyleOfRow(allOrdersHead, head);

        rowNum += 1;

        // Set up order rows
        for (Order o : reportSession.orders){
            Row orderRow = allOrdersSheet.createRow(rowNum);
            Cell orderId = orderRow.createCell(0);
            orderId.setCellValue(o.orderId);
            Cell orderType = orderRow.createCell(1);
            if (o.orderType == cash){
                orderType.setCellValue("Cash");
            }
            else if (o.orderType == eftpos)
            {
                orderType.setCellValue("Eftpos");
            }
            else {
                orderType.setCellValue("Mobile");
            }
            Cell orderName = orderRow.createCell(2);
            orderName.setCellValue(o.orderName);
            Cell orderStatus = orderRow.createCell(3);
            switch (o.orderStatus){
                case 1 : orderStatus.setCellValue("PENDING");
                    break;
                case 2 : orderStatus.setCellValue("CONFIRMED");
                    break;
                case 3 : orderStatus.setCellValue("READY TO COLLECT");
                    break;
                case 4 : orderStatus.setCellValue("COMPLETE");
                    break;
                default : orderStatus.setCellValue("CANCELLED");
                    break;
            }
            Cell orderTime = orderRow.createCell(4);
            orderTime.setCellValue(timeFormat.format(o.orderTime));
            Cell acceptTime = orderRow.createCell(5);
            if (o.acceptedTime != null) {
                acceptTime.setCellValue(timeFormat.format(o.acceptedTime));
            }
            Cell readyTime = orderRow.createCell(6);
            if (o.readyTime != null) {
                readyTime.setCellValue(timeFormat.format(o.readyTime));
            }
            Cell collectTime = orderRow.createCell(7);
            if (o.collectTime != null) {
                collectTime.setCellValue(timeFormat.format(o.collectTime));
            }
            Cell duration = orderRow.createCell(8);
            if (o.readyTime != null) {
                Long difference = (o.readyTime.getTime() - o.orderTime.getTime());
                Date diff = new Date(difference);
                duration.setCellValue(durationFormat.format(diff));
            }
            Cell comments = orderRow.createCell(9);
            comments.setCellValue(o.comments);
            Cell discount = orderRow.createCell(10);
            if (o.discount != null) {
                discount.setCellValue(o.discount.floatValue());
                discount.setCellStyle(money);
            }
            Cell totalPaid = orderRow.createCell(11);
            if (o.orderStatus == Order.STATUS_COMPLETED || o.orderStatus == Order.STATUS_READY_TO_COLLECT || o.orderStatus == Order.STATUS_CONFIRMED) {
                totalPaid.setCellValue(o.orderTotal.floatValue());
                totalPaid.setCellStyle(money);
            }
            Cell procFee = orderRow.createCell(12);
            if (o.orderType == mobile && (o.orderStatus == Order.STATUS_COMPLETED || o.orderStatus == Order.STATUS_READY_TO_COLLECT || o.orderStatus == Order.STATUS_CONFIRMED)){
                BigDecimal cut = new BigDecimal(0.035).setScale(3, RoundingMode.DOWN);
                BigDecimal fee = cut.multiply(o.orderTotal).setScale(2, RoundingMode.DOWN);
                fee = fee.add(new BigDecimal(0.17).setScale(2, RoundingMode.DOWN));
                procFee.setCellValue(fee.floatValue());
                procFee.setCellStyle(money);
            }
            Cell smsFee = orderRow.createCell(13);
            if (o.orderType != mobile && !o.mobileNumber.isEmpty() && o.mobileNumber != null){
                smsFee.setCellValue(0.05);
                smsFee.setCellStyle(money);
            }
            rowNum += 1;
        }
    }

    private void prepareItemsSheet(){
        short rowNum = 0;

        // Set up header values
        Row itemHeadRow = allItemsSheet.createRow(rowNum);
        Cell orderItemIdHead = itemHeadRow.createCell(0);
        orderItemIdHead.setCellValue("Order Id");
        Cell itemIdHead = itemHeadRow.createCell(1);
        itemIdHead.setCellValue("Item Name");
        Cell priceHead = itemHeadRow.createCell(2);
        priceHead.setCellValue("Price");
        Cell qtyHead = itemHeadRow.createCell(3);
        qtyHead.setCellValue("Quantity");
        Cell paidHead = itemHeadRow.createCell(4);
        paidHead.setCellValue("Paid (inc options)");
        setStyleOfRow(itemHeadRow, head);
        rowNum += 1;

        for (Order o : reportSession.orders){
            for (OrderItem oi : o.orderItems){
                Row itemRow = allItemsSheet.createRow(rowNum);
                Cell orderId = itemRow.createCell(0);
                orderId.setCellValue(o.orderId);
                Cell itemName = itemRow.createCell(1);
                itemName.setCellValue(oi.item.name);
                Cell price = itemRow.createCell(2);
                price.setCellValue(oi.item.price.floatValue());
                price.setCellStyle(money);
                Cell quantity = itemRow.createCell(3);
                quantity.setCellValue(oi.quantity);
                Cell paid = itemRow.createCell(4);
                paid.setCellValue(oi.totalPaid.multiply(new BigDecimal(oi.quantity)).floatValue());
                paid.setCellStyle(money);
                rowNum += 1;
            }
        }
    }

    private BigDecimal getTotalValueOfType(int type){
        BigDecimal total = new BigDecimal(0);
        for (Order o : reportSession.orders){
            if (o.orderType == type && (o.orderStatus == Order.STATUS_READY_TO_COLLECT || o.orderType == type && o.orderStatus == Order.STATUS_COMPLETED || o.orderStatus == Order.STATUS_CONFIRMED)){
                total = total.add(o.orderTotal);
            }
        }
        return total;
    }

    private String getAverageOrderTime(int type){
        Long difference = new Long(0);
        int count = 0;
        for (Order o : reportSession.orders){
            if (o.orderType == type && o.orderStatus == Order.STATUS_COMPLETED){
                difference += (o.readyTime.getTime() - o.orderTime.getTime());
                count = count + 1;
            }
        }
        Long avgDiff;
        if (count > 0) {
            avgDiff = difference / count;
        }
        else {
            avgDiff = difference;
        }
        Date diffTime = new Date(avgDiff);
        return durationFormat.format(diffTime);
    }

    private BigDecimal getProcessingFees(){
        BigDecimal fees = new BigDecimal(0.17 * reportSession.orderCountOfType(mobile)).setScale(2, RoundingMode.DOWN);
        BigDecimal totalPercentage = getTotalValueOfType(mobile).multiply(new BigDecimal(0.035).setScale(3, RoundingMode.FLOOR));
        fees = fees.add(totalPercentage);
        return fees.setScale(2, RoundingMode.FLOOR);
    }

    private BigDecimal getSMSFees(){
        Long smsOrderCount = reportSession.orderCountofSMSOrders();
        BigDecimal smsCost = new BigDecimal(0.05).setScale(2, RoundingMode.FLOOR);
        return smsCost.multiply(new BigDecimal(smsOrderCount)).setScale(2, RoundingMode.FLOOR);
    }

    private static void setStyleOfRow(Row row, CellStyle style){
        for(int i = 0; i < row.getLastCellNum(); i++){//For each cell in the row
            row.getCell(i).setCellStyle(style);//Set the style
        }
    }

    public void send() throws IOException, EmailException {
        generateFile();
        this.sendReport();
    }

    public String generateFile() throws IOException {
        out = new FileOutputStream(this.path);
        this.prepareSummarySheet();
        this.prepareAllOrders();
        this.prepareItemsSheet();
        int columnCountSummary = 6;
        int columnCountOrders = 14;
        int columnCountItems = 5;
        for (int i = 0; i < columnCountSummary; i++){
            summarySheet.autoSizeColumn(i);
        }
        for (int i = 0; i < columnCountOrders; i++){
            allOrdersSheet.autoSizeColumn(i);
        }
        for (int i = 0; i < columnCountItems; i++){
            allItemsSheet.autoSizeColumn(i);
        }

        wb.write(out);
        out.flush();
        out.close();
        return this.path;
    }

    private void sendReport() throws EmailException {
        BigDecimal totalRemitted = getTotalValueOfType(mobile).subtract(getProcessingFees().add(getSMSFees()));

        String trelloCardSummary = "totalRemitted: " + totalRemitted + ",\n"
                + "Total Mobile: " + getTotalValueOfType(mobile) + ",\n"
                + "Total Cash: " + getTotalValueOfType(cash) + ",\n"
                + "Total Eftpos: " + getTotalValueOfType(eftpos) + ",\n"
                + "Total Processing: " + getProcessingFees() + ",\n"
                + "Total SMS: " + getSMSFees() + ",\n";

        F.Promise<HashMap> promise = TrelloHandler.createCard("New Trucksession " + reportSession.truckSessionId + "for " + truckName,
                trelloCardSummary);

        //getTotalValueOfType(cash)
        HashMap trelloResponse = promise.get();

        Date today = new Date();
        String fileName = reportSession.truckSessionId + reportSession.truck.name + dateFormat.format(reportSession.startTime) + ".xls";
        String subject =  this.truckName + ", your report for " + dateFormat.format(today) + " is ready!";
        String message = "Hi " + this.truckName + ",\n \n" + "Attached is your report for " + dateFormat.format(today) + ".\n \n"
                + "If you have any questions please contact us."
                + "\n \n"
                + "Thanks,\nStreetEats Team";
        //

        MailHandler.sendEmailWithAttachment(this.sendAddress, subject, message, this.path, fileName);
    }
}
