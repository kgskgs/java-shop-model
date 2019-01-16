/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.audit;

import java.sql.Connection;
import java.util.Date;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import shop.infrastructure.MySqlRepository;
import shop.models.BoughtProduct;
import shop.models.Employee;
import shop.models.Product;
import shop.models.Receipt;
import shop.models.Shop;

/**
 *
 * @author Lyuboslav
 */
public class Report{
    private final Connection sqlConnection;
    
    public Report(Connection sqlConnection)
    {
        this.sqlConnection = sqlConnection;        
    }     
    
    public ArrayList<String> WriteReportReceipts(Employee employee, Date startDate, Date endDate)
    {
        try {
            ArrayList<String> result = new ArrayList<>();
            
            MySqlRepository<Receipt> receiptRepo = new MySqlRepository<>(Receipt.class,sqlConnection,null);
            
            ArrayList<Receipt> receipts = receiptRepo.GetByForeignKey(Employee.class,employee.employeeId);
            
            DateFormat format;
            format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss", Locale.ENGLISH);
            
            for (Receipt receipt : receipts){
                Date receiptDate = (Date) format.parse(receipt.buyDate);
                //&& receiptDate.before(endDate) && receiptDate.after(startDate)
                if (receipt.employeeId == employee.employeeId ) {
                    //thisEmployeeReceipts.add(receipt);
                }
            }
            
            MySqlRepository<BoughtProduct> boughtProductsRepo = new MySqlRepository<>(BoughtProduct.class,sqlConnection,null);
            MySqlRepository<Product> productsRepo = new MySqlRepository<>(Product.class,sqlConnection,null);
            
            for (Receipt receipt : receipts){
                result.add("Receipt " + receipt.receiptId + " " + receipt.buyDate);
                ArrayList<BoughtProduct> boughtProducts = boughtProductsRepo.GetByForeignKey(Receipt.class, receipt.receiptId);
                
                for(BoughtProduct boughtProduct : boughtProducts){
                    if (boughtProduct.receiptId == receipt.receiptId) {
                        Product product = productsRepo.Get(boughtProduct.productId);
                        result.add(product.productName + " " + product.price);
                    }
                }
            }
            
            return result;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        } catch (ParseException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    public double ReceiptTotal(Receipt rec, Date startDate, Date endDate) throws SQLException{
        double total = 0.0;
        try {
            DateFormat format;
            format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss", Locale.ENGLISH);
            Date receiptDate = (Date) format.parse(rec.buyDate);
            if (!receiptDate.before(endDate) && !receiptDate.after(startDate)) 
                return 0.0;
            
            

            MySqlRepository<BoughtProduct> boughtProductsRepo = new MySqlRepository<>(BoughtProduct.class,sqlConnection,null);
            MySqlRepository<Product> productsRepo = new MySqlRepository<>(Product.class,sqlConnection,null);

            ArrayList<BoughtProduct> boughtProducts = boughtProductsRepo.GetByForeignKey(Receipt.class, rec.receiptId);
            for(BoughtProduct boughtProduct : boughtProducts){
                if (boughtProduct.receiptId == rec.receiptId) {
                    Product product = productsRepo.Get(boughtProduct.productId);
                    total+=product.price;
                }
            }            
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
        
        return total;           

    }
    
    public double EmployeeTotal(Employee emp, Date startDate, Date endDate) throws SQLException
    {        
        double total = 0.0;
        for (Receipt receipt : new MySqlRepository<>(Receipt.class,sqlConnection,null).GetByForeignKey(Employee.class, emp.employeeId)){                            
            total += ReceiptTotal(receipt, startDate, endDate);                 
        }
        return total;
    }
    
    public ArrayList<String> ReportShopEmployee(Employee employee, Date startDate, Date endDate){
        
        try {
            ArrayList<String> result = new ArrayList<>();
            
            MySqlRepository<Receipt> receiptRepo = new MySqlRepository<>(Receipt.class,sqlConnection,null);
            
            ArrayList<Receipt> receipts = receiptRepo.GetByForeignKey(Employee.class,employee.employeeId);
            
            DateFormat format;
            format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss", Locale.ENGLISH);
            
            for (Receipt receipt : receipts){
                try {
                    Date receiptDate = (Date) format.parse(receipt.buyDate);
                } catch (ParseException ex) {
                    ex.printStackTrace();
                }
                //&& receiptDate.before(endDate) && receiptDate.after(startDate)
                if (receipt.employeeId == employee.employeeId ) {
                    //thisEmployeeReceipts.add(receipt);
                }
            }
            
            MySqlRepository<BoughtProduct> boughtProductsRepo = new MySqlRepository<>(BoughtProduct.class,sqlConnection,null);
            MySqlRepository<Product> productsRepo = new MySqlRepository<>(Product.class,sqlConnection,null);
            
            double totalTotal = 0.0;
            for (Receipt receipt : receipts){
                
                double total = ReceiptTotal(receipt, startDate,endDate);
                result.add("Receipt " + receipt.receiptId + " " + receipt.buyDate + " TOTAL: " + total);
                totalTotal += total;
            }
            result.add("Very total: " + totalTotal);
            return result;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    public ArrayList<String> ReportShop(Shop shop, Date startDate, Date endDate)
    {
        ArrayList<String> result = new ArrayList<>();
        
        ArrayList<Employee> employees;
        try {
            employees = new MySqlRepository(Employee.class,sqlConnection,null).GetByForeignKey(Shop.class, shop.shopId);
         
            double totaltotal = 0.0;
                
            for (Employee emp : employees) {      
                double total = EmployeeTotal(emp, startDate, endDate);
                result.add(emp.firstname + " " + emp.lastname + " Total: " + total);
                totaltotal+=total;
            } 
            
            result.add(shop.shopName + " total: " + totaltotal);
        }
        catch (SQLException ex) {
                ex.printStackTrace();
        }
        
        return result;
    }
    
    public ArrayList<String> ReportAllShops(Date startDate, Date endDate)
    {
        ArrayList<String> result = new ArrayList<>();
        
        try {
            ArrayList<Shop> shops = new MySqlRepository<>(Shop.class,sqlConnection,null).GetAll(0, 1000, false);
            double totaltotal = 0.0;
            for(Shop shop : shops){
                ArrayList<Employee> employees = new MySqlRepository<>(Employee.class,sqlConnection,null).GetByForeignKey(Shop.class, shop.shopId);
                double total = 0.0f;
                for(Employee emp : employees){
                    total += EmployeeTotal(emp, startDate, endDate);                    
                }   
                result.add("Shop " + shop.shopName + " Total:" + total);
                totaltotal+=total;
            }
            result.add("All shops total: " + totaltotal);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }        
        
        return result;
    }
}
