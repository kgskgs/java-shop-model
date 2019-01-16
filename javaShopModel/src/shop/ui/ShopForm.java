/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.util.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.RowFilter.ComparisonType;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import shop.audit.Report;
import shop.db.*;
import shop.infrastructure.MySqlRepository;
import shop.models.*;
import shop.models.compare.ShopSortById;

/**
 * 
 * @author K1
 */
public class ShopForm extends javax.swing.JFrame implements Runnable {

    //db custom vars declaration
    private final BlockingQueue<String> queryStrQueue;
    private Connection DBconnection;
    private DBwriteThread dbWrite;
    
    private final DateTimeFormatter DBtimeFormat;
    
    //internal flags
    private boolean loggedIn = false;
    private int focusedTab = 0;
    private int usrRank = 0;
    
    public static final int PNL_SHOPS = 0;
    public static final int PNL_PRODUCTS = 1;
    public static final int PNL_CHECKOUT = 2;
    public static final int PNL_REPORTS = 3;
    
    private double chkTotal = 0.00;
    
    private Employee me;
    
    private HashMap <Integer, Shop> shopsDict;
    private HashMap <Integer, String> prodNames;
    
    //model entities
    //invoice
    private ArrayList<Client> clients;
    private MySqlRepository<Client> clientRepository;
    private MySqlRepository<Invoice> invoiceRepository;
    
    //products & categories
    private ArrayList<Category> categories;
    private ArrayList<Product> products;
    private MySqlRepository<Category> catRepository;
    private MySqlRepository<Product> prodRepository;
    
    //shops & employess
    private ArrayList<Shop> shops;
    private ArrayList<Employee> employees;
    private MySqlRepository<Shop> shopRepository;
    private MySqlRepository<Employee> empRepository;
    
    //receipt
    private MySqlRepository<Receipt> receiptRepository;
    private MySqlRepository<BoughtProduct> boughtRepository;
     
    //ui representations of models
    private final DefaultListModel<String> lstCatsM;
    private final DefaultTableModel tblProdsM;
    private final TableRowSorter<? extends TableModel>  tblProdsRS;
    private final DefaultListSelectionModel tblProdsSM;
    private final ListSelectionListener prodsListener;
    
    private final DefaultListModel<String> lstShopsM;
    private final DefaultTableModel tblEmpsM;
    private final TableRowSorter<? extends TableModel>  tblEmpsRS;
    private final DefaultListSelectionModel tblEmpsSM;
    private final ListSelectionListener empsListener;
    
    private final DefaultTableModel tblChkM;
    private final TableRowSorter<? extends TableModel>  tblChkRS;
    
    private final DocChangeListener filterDocListener;
    //private final TableModelListener tblChkML;
    

    /**
     * Creates new form ShopForm, redirects os to doc
     */  
    public ShopForm() {
        //run generated code
        initComponents();
        //get needed vars from generated code
        //this.lstProdsM = (DefaultListModel<String>) lstProds.getModel();
        lstCatsM = (DefaultListModel<String>) lstCats.getModel();
        tblProdsM = (DefaultTableModel) tblProds.getModel();
        tblProdsSM = (DefaultListSelectionModel) tblProds.getSelectionModel();
        tblProdsRS = (TableRowSorter) tblProds.getRowSorter(); 
        
        lstShopsM = (DefaultListModel<String>) lstShops.getModel();
        tblEmpsM = (DefaultTableModel) tblEmps.getModel();
        tblEmpsSM = (DefaultListSelectionModel) tblEmps.getSelectionModel();
        tblEmpsRS = (TableRowSorter) tblEmps.getRowSorter();
        
        tblChkM = (DefaultTableModel) tblChk.getModel();
        tblChkRS = (TableRowSorter) tblChk.getRowSorter();
        
        shopsDict = new HashMap<>();
        prodNames = new HashMap<>();
        
        //listeners not provided by netbeans gui
        prodsListener = new ListSelectionListener () {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                tblProdsListSelection(e);
            }
        };
        
        empsListener = new ListSelectionListener () {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                tblEmpsListSelection(e);
            }
        };
        
        filterDocListener = new DocChangeListener () {
            @Override
            public void docChanged(DocumentEvent e){
                setChkFilter();
            }
        };
        
        AbstractAction tblChkCellChange = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                calcPrice((TableCellListener)e.getSource());
            }
        };
        
        TableCellListener tcl = new TableCellListener(tblChk, tblChkCellChange);
        
        txtFilterCatId.getDocument().addDocumentListener(filterDocListener);
        txtFilterCatName.getDocument().addDocumentListener(filterDocListener);
        txtFilterProdId.getDocument().addDocumentListener(filterDocListener);
        txtFilterProdName.getDocument().addDocumentListener(filterDocListener);
      
        //set up log textarea
        //PrintStream os = new PrintStream(new LogDocStream(txtLog.getDocument()), true);
        //System.setOut(os);
        //System.setErr(os);
        
        //set up database connection vars
        queryStrQueue = new LinkedBlockingQueue<>();  
        
        DBtimeFormat = DateTimeFormatter.ofPattern("YYYY-MM-dd HH:mm:ss");
        
        System.out.println("started");
    }
    
    
    /**
    * Connect to the shop database (db name hardcoded)
    * create thread for writing to db and entity repositories
    * @param usr username
    * @param pass password
    * @param port port
    * @return true on success
    */ 
    public boolean connect(String usr, String pass, String port){
        try {
            DBconnection = ConnectionFactory.getConnection("javashopmodeldb", usr, pass, port);
            
            
            dbWrite = new DBwriteThread(DBconnection, queryStrQueue);
            dbWrite.start();
            
            clientRepository = new MySqlRepository<>(Client.class, DBconnection, queryStrQueue);
            invoiceRepository = new MySqlRepository<>(Invoice.class, DBconnection, queryStrQueue);
            catRepository = new MySqlRepository<>(Category.class, DBconnection, queryStrQueue);
            prodRepository = new MySqlRepository<>(Product.class, DBconnection, queryStrQueue);
            shopRepository = new MySqlRepository<>(Shop.class, DBconnection, queryStrQueue);
            empRepository = new MySqlRepository<>(Employee.class, DBconnection, queryStrQueue);
            receiptRepository = new MySqlRepository<>(Receipt.class, DBconnection, queryStrQueue);
            boughtRepository = new MySqlRepository<>(BoughtProduct.class, DBconnection, queryStrQueue);
            
            usrRank = dbWrite.getUniqueInt("accessLvl", "employees", "username", usr);
            
            System.out.println("connected to database as " + usr + " with access level " + usrRank);
           
            return true;
        } catch (ClassNotFoundException | SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    /**
     * runs when user is logging out, 
     */
    private void exitProcedure(){
        if (loggedIn){
            switch(focusedTab){
                case PNL_SHOPS: pnlShopsExit(); break;
                case PNL_PRODUCTS: pnlProductsExit(); break;
                case PNL_CHECKOUT: pnlCheckoutExit(); break;
            }
            queryStrQueue.add("fc");
            //send end signal and wait for thread to close
            queryStrQueue.add("end");
            try {
                dbWrite.join();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    //ENTITY OPERATIONS
    public void pnlShopsEnter(){
        try {
            System.out.println(txtUser.getText());
            
            lstShopsM.addElement("New Shop");
        
            shops = shopRepository.GetAll(0, 1000, true);
            employees = empRepository.GetAll(0, 1000, true);
            
            Collections.sort(shops, new ShopSortById());
            
            for (Shop s : shops) {
                lstShopsM.addElement(s.shopId+" - " + s.shopName);
                cmbEmpShop.addItem(Integer.toString(s.shopId)+" - "+s.shopName);
            }
                       
            tblEmpsM.addRow(new Object[] {-1, "New Employee"});       
            for (Employee e : employees){
                tblEmpsM.addRow(new Object[] {e.shopId, Integer.toString(e.employeeId) +" - "+ e.firstname});
                //store currently logged in employee
                if (e.username.equals(txtUser.getText()))
                    me = e;
            }
            
            tblEmpsSM.addListSelectionListener(empsListener);
            lstShops.setSelectedIndex(0);
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        } 
    }
    
    public void pnlShopsExit(){
        tblEmpsSM.removeListSelectionListener(empsListener);
 
        updateModels(employees, empRepository);
        updateModels(shops, shopRepository);
        
        employees.clear();
        
        cmbEmpShop.removeAllItems();
        lstShopsM.removeAllElements();
        clearTable(tblEmpsM);    
    }
    
    /**
     * run on entering 'products' tab
     * load products and categories
     */
    public void pnlProductsEnter(){
        try {
            lstCatsM.addElement("New Category");
            
            categories = catRepository.GetAll(0, 1000, true);
            products = prodRepository.GetAll(0, 1000, true);
            
            for (Category c : categories) {
                lstCatsM.addElement(c.productCategoriyId+" - " + c.categoryName);
            }
                       
            tblProdsM.addRow(new Object[] {-1, "New Product"});       
            for (Product p : products){
                tblProdsM.addRow(new Object[] {p.categoryId, Integer.toString(p.productId) +" - "+ p.productName});
            }
            
            tblProdsSM.addListSelectionListener(prodsListener);
            lstCats.setSelectedIndex(0);
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * run when exiting tab 'products'
     * empty used variables and reset ui; update models to db
     */
    public void pnlProductsExit(){
        tblProdsSM.removeListSelectionListener(prodsListener);
          
        updateModels(products, prodRepository);
        updateModels(categories, catRepository);
        
        products.clear();
        
        lstCatsM.removeAllElements();
        clearTable(tblProdsM);   
    }
    
    
    /**
     * run when entering tab 'checkout'
     * 
     */
    public void pnlCheckoutEnter(){   
        //load invoice panel
        try {
            clients = clientRepository.GetAll(0, 1000, false);
            categories = catRepository.GetAll(0, 1000, true);
            products = prodRepository.GetAll(0, 1000, true);
            
            //get category names easily accessible
            HashMap <Integer, String> catNames = new HashMap<>();
            for (Category c: categories){
                catNames.put(c.productCategoriyId, c.categoryName);
            }
            for (Shop s: shops){
                shopsDict.put(s.shopId, s);
            }
            
            //populate checkout table
            for (Product p: products){
                tblChkM.addRow(new Object[]{
                                p.categoryId,
                                catNames.get(p.categoryId),
                                p.productId,
                                p.productName,
                                0,
                                p.price
                });
                prodNames.put(p.productId, p.productName);
            }

            for (Client c: clients){
                cmbInvCname.addItem(c.clientId+": " +c.companyName +" - "+ c.firstname);
            }  
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * run when tab switched away from 'checkout'
     * empty used variables and reset ui; update models to db
     */
    public void pnlCheckoutExit(){
        
        clearTable(tblChkM);
        
        
        //Invoice panel
        //ui
        if(chkInvoice.isSelected())
            chkInvoice.doClick(); //triggers the action event listener
        //reset combobox
        cmbInvCname.removeAllItems();
        cmbInvCname.addItem("New Client");
        
        //update entities
        updateModels(clients, clientRepository);
        
        //we'll be reloading these try to free memory while not in use
        clients.clear();
    }
    
    
    private void pnlReportsEnter() {
        try {
            shops = shopRepository.GetAll(0, 1000, false);
            employees = empRepository.GetAll(0, 1000, false);
            cmbRepShop.addItem("All");
            for(Shop shop : shops){
                cmbRepShop.addItem(shop.shopName);
            }
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        
    }
    
    /**
     * Display client information in the relevant ui textfields
     * @param c client to load
     */
    private void loadClient(Client c){
        txtInvCname.setText(c.companyName);
        txtInvFname.setText(c.firstname);
        txtInvLname.setText(c.lastname);
        txtInvEIK.setText(c.eik);
    }
    
    /**
     * Display category information in the relevant ui textfields
     * @param c category to load
     */
    private void loadCategory(Category c){
        txtCatName.setText(c.categoryName);
        txtCatId.setText(Integer.toString(c.productCategoriyId));
        txtCatDesc.setText(c.description);
    }
    
    /**
     * Display product information in the relevant ui textfields
     * @param p product to load
     */
    private void loadProduct(Product p){
        txtProdName.setText(p.productName);
        txtProdPrice.setText(String.valueOf(p.price));
        txtProdDesc.setText(p.description);
        String prodIdText = (p.productId==-1)? "pending" : Integer.toString(p.productId);
        txtProdId.setText(prodIdText);
    }
    
    /**
     * Display shop information in the relevant ui textfields
     * @param s shop to load
     */
    private void loadShop(Shop s){
        txtShopName.setText(s.shopName);
        txtShopAdress.setText(s.address);
        txtShopId.setText(Integer.toString(s.shopId));
    }
    
    /**
     * Display employee information in the relevant ui components
     * @param e employee to load
     */
    private void loadEmp(Employee e){
        txtEmpName1.setText(e.firstname);
        txtEmpName2.setText(e.lastname);
        txtEmpUsr.setText(e.username);
        cmbEmpRank.setSelectedIndex(e.accessLvl);
        cmbEmpShop.setSelectedIndex(e.shopId-1);
        txtEmpId.setText(Integer.toString(e.employeeId));
    }
    
    /**
     * checks which models from a list need to be updated, and calls the repository update functions
     * @param models list of models
     * @param rep java repository class for the models
     */
    private void updateModels(ArrayList<? extends Model> models, MySqlRepository rep){
        models.forEach((Model m) -> {
            if (m.isNewInstance()){
                rep.Insert(m);
                m.setNewInstance(false);
                m.setUpdated(false);
            } else if (m.isUpdated()){
                rep.Update(m);
                m.setNewInstance(false);
                m.setUpdated(false);
            }
        });
    }
    
    //UI HELPER FUNCTIONS
    /**
     * swap two components in a layered pane
     * @param lp layered pane that contains the components
     * @param a component 1
     * @param b component 2
     */
    private void swapLayers(JLayeredPane lp, Component a, Component b){
        int layerA = lp.getLayer(a);
        int layerB = lp.getLayer(b);
        lp.setLayer(a, layerB);
        lp.setLayer(b, layerA);
    }

    /**
     * setEnabled for all components in a panel
     * @param pnl target panel
     * @param enaled value to set
     */
    private void setEnabledPanel(JPanel pnl, boolean enabled){
        for (Component c : pnl.getComponents()){
            c.setEnabled(enabled);
        }
    }
    
    /**
     * Clear all textfields in a panel
     * @param pnl panel to be cleared
     */
    private void clearTxts(JPanel pnl){
       for (Component c : pnl.getComponents()){
           if (c instanceof JTextField){
               ((JTextField) c).setText("");
           } else if (c instanceof JScrollPane){
               Component c1 = ((JScrollPane)c).getViewport().getComponent(0);
               ((JTextArea) (c1)).setText("");
           }
       }
    }
    
    /**
     * Sets enabled parameter for all text fields in a JPanel
     * @param pnl panel whose components to set
     * @param enabled 
     */
    private void setTxtEnabled(JPanel pnl, boolean enabled){
        for (Component c : pnl.getComponents()){
            if (c instanceof JTextField)
                c.setEnabled(enabled);
            if (c instanceof JScrollPane){
               Component c1 = ((JScrollPane)c).getViewport().getComponent(0);
               c1.setEnabled(enabled);
           }
        }
    }
    
    /**
     * filters a jtable by the values of a single column
     * @param rs row sorter for the table
     * @param vals arrays of values to filter by (exact matches)
     * @param colIndex index of the column to search in  
     */
    private void setRowFilterInt(TableRowSorter<? extends TableModel> rs, int[] vals, int colIndex){
        ArrayList<RowFilter<Object,Object>> allFilters = new ArrayList<>();
        for (int i = 0; i < vals.length; i++){
            allFilters.add(RowFilter.numberFilter(ComparisonType.EQUAL, vals[i], colIndex));
        }
        rs.setRowFilter(RowFilter.orFilter(allFilters));
    }
    
     /**
     * remove all the rows from a jtable
     * @param tm model of the table
     */
    private void clearTable(DefaultTableModel tm){
        for (int i = tm.getRowCount()-1; i >= 0; i--)
            tm.removeRow(i);
    }
        
    /**
     * set the filter to the checkout table from all 4 textfields
     */
    private void setChkFilter(){
        ArrayList<RowFilter<Object,Object>> allFilters = new ArrayList<>();
        String catId = txtFilterCatId.getText();
        String catName = txtFilterCatName.getText();
        String prodId = txtFilterProdId.getText();
        String prodName = txtFilterProdName.getText();
        
        if (catId.length() > 0)
            allFilters.add(RowFilter.regexFilter("^"+catId, 0));
        else
            allFilters.add(RowFilter.regexFilter(".*", 0));
        if (catName.length() > 0)
            allFilters.add(RowFilter.regexFilter("^"+catName, 1));
        else
            allFilters.add(RowFilter.regexFilter(".*", 1));
        if (prodId.length() > 0)
            allFilters.add(RowFilter.regexFilter("^"+prodId, 2));
        else
            allFilters.add(RowFilter.regexFilter(".*", 2));
        if (prodName.length() > 0)
            allFilters.add(RowFilter.regexFilter("^"+prodName, 3));
        else
            allFilters.add(RowFilter.regexFilter(".*", 3));
        
        if (allFilters.size() > 0)
            tblChkRS.setRowFilter(RowFilter.andFilter(allFilters));
    }
    
    @Override
    public void run() {
        setVisible(true);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        txtLog = new javax.swing.JTextArea();
        lpMain = new javax.swing.JLayeredPane();
        tbtMain = new javax.swing.JTabbedPane();
        pnlShops = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblEmps = new javax.swing.JTable();
        jScrollPane3 = new javax.swing.JScrollPane();
        lstShops = new javax.swing.JList<>();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        pnlDetailsShop = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        btnShopUpdate = new javax.swing.JButton();
        btnShopNew = new javax.swing.JButton();
        btnShopDelete = new javax.swing.JButton();
        txtShopName = new javax.swing.JTextField();
        txtShopAdress = new javax.swing.JTextField();
        jLabel27 = new javax.swing.JLabel();
        txtShopId = new javax.swing.JTextField();
        pnlDetailEmp = new javax.swing.JPanel();
        btnEmpUpdate = new javax.swing.JButton();
        btnEmpNew = new javax.swing.JButton();
        btnEmpDelete = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        txtEmpName1 = new javax.swing.JTextField();
        txtEmpName2 = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        txtEmpUsr = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        cmbEmpShop = new javax.swing.JComboBox<>();
        jLabel8 = new javax.swing.JLabel();
        cmbEmpRank = new javax.swing.JComboBox<>();
        jLabel28 = new javax.swing.JLabel();
        txtEmpId = new javax.swing.JTextField();
        pnlProducts = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        tblProds = new javax.swing.JTable();
        jScrollPane5 = new javax.swing.JScrollPane();
        lstCats = new javax.swing.JList<>();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        pnlDetailsCat = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        btnCatUpdate = new javax.swing.JButton();
        btnCatNew = new javax.swing.JButton();
        btnCatDelete = new javax.swing.JButton();
        txtCatName = new javax.swing.JTextField();
        txtCatId = new javax.swing.JTextField();
        jScrollPane6 = new javax.swing.JScrollPane();
        txtCatDesc = new javax.swing.JTextArea();
        jLabel25 = new javax.swing.JLabel();
        pnlDetailProd = new javax.swing.JPanel();
        btnProdUpdate = new javax.swing.JButton();
        btnProdNew = new javax.swing.JButton();
        btnProdDelete = new javax.swing.JButton();
        jLabel13 = new javax.swing.JLabel();
        txtProdName = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        txtProdPrice = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        txtProdId = new javax.swing.JTextField();
        jScrollPane7 = new javax.swing.JScrollPane();
        txtProdDesc = new javax.swing.JTextArea();
        jLabel26 = new javax.swing.JLabel();
        pnlCheckout = new javax.swing.JPanel();
        tblSP = new javax.swing.JScrollPane();
        tblChk = new javax.swing.JTable();
        chkInvoice = new javax.swing.JCheckBox();
        btnCheckDone = new javax.swing.JButton();
        pnlInvoice = new javax.swing.JPanel();
        jLabel16 = new javax.swing.JLabel();
        cmbInvCname = new javax.swing.JComboBox<>();
        txtInvCname = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        txtInvEIK = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        txtInvFname = new javax.swing.JTextField();
        jLabel19 = new javax.swing.JLabel();
        txtInvLname = new javax.swing.JTextField();
        pnlChkFilters = new javax.swing.JPanel();
        txtFilterCatId = new javax.swing.JTextField();
        jLabel29 = new javax.swing.JLabel();
        jLabel31 = new javax.swing.JLabel();
        txtFilterProdId = new javax.swing.JTextField();
        txtFilterCatName = new javax.swing.JTextField();
        txtFilterProdName = new javax.swing.JTextField();
        jLabel30 = new javax.swing.JLabel();
        jLabel32 = new javax.swing.JLabel();
        btnClearCatF = new javax.swing.JButton();
        btnClearProdF = new javax.swing.JButton();
        jLabel33 = new javax.swing.JLabel();
        lblChkTotal = new javax.swing.JLabel();
        btnChkClear = new javax.swing.JButton();
        pnlReports = new javax.swing.JPanel();
        jLabel20 = new javax.swing.JLabel();
        cmbRepShop = new javax.swing.JComboBox<>();
        jLabel21 = new javax.swing.JLabel();
        cmbRepEmp = new javax.swing.JComboBox<>();
        jLabel22 = new javax.swing.JLabel();
        txtRepFrom = new javax.swing.JTextField();
        jLabel23 = new javax.swing.JLabel();
        txtRepTo = new javax.swing.JTextField();
        chkRepInv = new javax.swing.JCheckBox();
        txtRepEIK = new javax.swing.JTextField();
        bntRepGet = new javax.swing.JButton();
        jLabel24 = new javax.swing.JLabel();
        pnlLogin = new javax.swing.JPanel();
        txtUser = new javax.swing.JTextField();
        txtPass = new javax.swing.JPasswordField();
        lblUser = new javax.swing.JLabel();
        lblPass = new javax.swing.JLabel();
        btnLogin = new javax.swing.JButton();
        lblPort = new javax.swing.JLabel();
        txtPort = new javax.swing.JTextField();
        jMenuBar1 = new javax.swing.JMenuBar();
        mnuFileGrp = new javax.swing.JMenu();
        mnuSaveLog = new javax.swing.JMenuItem();
        mnuCommit = new javax.swing.JMenuItem();
        mnuExit = new javax.swing.JMenuItem();
        mnuUserGrp = new javax.swing.JMenu();
        mnuUinfo = new javax.swing.JMenuItem();
        mnuChangePass = new javax.swing.JMenuItem();
        mnuLogout = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setFocusCycleRoot(false);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        txtLog.setEditable(false);
        txtLog.setColumns(20);
        txtLog.setFont(new java.awt.Font("Consolas", 0, 12)); // NOI18N
        txtLog.setRows(5);
        txtLog.setDisabledTextColor(new java.awt.Color(0, 0, 0));
        jScrollPane1.setViewportView(txtLog);

        tbtMain.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tbtMainStateChanged(evt);
            }
        });

        tblEmps.setAutoCreateRowSorter(true);
        tblEmps.setModel(new javax.swing.table.DefaultTableModel(0, 2));
        tblEmps.setAutoscrolls(false);
        tblEmps.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(tblEmps);
        tblEmps.setTableHeader(null); //remove header
        //hide first column
        javax.swing.table.TableColumnModel tcm2 = tblEmps.getColumnModel();
        tcm2.removeColumn( tcm2.getColumn(0) );
        //make table not editable
        tblEmps.setDefaultEditor(Object.class, null);

        lstShops.setModel(new javax.swing.DefaultListModel<String>());
        lstShops.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstShops.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lstShopsValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(lstShops);

        jLabel1.setText("Shops");

        jLabel2.setText("Employees");

        pnlDetailsShop.setBorder(javax.swing.BorderFactory.createTitledBorder("Shop Details"));

        jLabel3.setText("Name");

        jLabel4.setText("Adress");

        btnShopUpdate.setText("Update");
        btnShopUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnShopUpdateActionPerformed(evt);
            }
        });

        btnShopNew.setText("New");
        btnShopNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnShopNewActionPerformed(evt);
            }
        });

        btnShopDelete.setText("Delete");
        btnShopDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnShopDeleteActionPerformed(evt);
            }
        });

        jLabel27.setText("ID");

        txtShopId.setEnabled(false);

        javax.swing.GroupLayout pnlDetailsShopLayout = new javax.swing.GroupLayout(pnlDetailsShop);
        pnlDetailsShop.setLayout(pnlDetailsShopLayout);
        pnlDetailsShopLayout.setHorizontalGroup(
            pnlDetailsShopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlDetailsShopLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(btnShopUpdate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnShopNew)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnShopDelete)
                .addGap(0, 28, Short.MAX_VALUE))
            .addGroup(pnlDetailsShopLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlDetailsShopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel27)
                    .addComponent(jLabel4)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlDetailsShopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtShopAdress)
                    .addComponent(txtShopName)
                    .addComponent(txtShopId))
                .addContainerGap())
        );
        pnlDetailsShopLayout.setVerticalGroup(
            pnlDetailsShopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlDetailsShopLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlDetailsShopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtShopName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlDetailsShopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(txtShopAdress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlDetailsShopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel27)
                    .addComponent(txtShopId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(pnlDetailsShopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnShopUpdate)
                    .addComponent(btnShopNew)
                    .addComponent(btnShopDelete))
                .addGap(7, 7, 7))
        );

        pnlDetailEmp.setBorder(javax.swing.BorderFactory.createTitledBorder("Employee Details"));

        btnEmpUpdate.setText("Update");
        btnEmpUpdate.setEnabled(false);
        btnEmpUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEmpUpdateActionPerformed(evt);
            }
        });

        btnEmpNew.setText("New");
        btnEmpNew.setEnabled(false);
        btnEmpNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEmpNewActionPerformed(evt);
            }
        });

        btnEmpDelete.setText("Delete");
        btnEmpDelete.setEnabled(false);
        btnEmpDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEmpDeleteActionPerformed(evt);
            }
        });

        jLabel5.setText("Name");

        jLabel7.setText("Username");

        jLabel6.setText("Shop");

        jLabel8.setText("Position");

        cmbEmpRank.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Cashier", "Cashier/Manager" }));

        jLabel28.setText("ID");

        txtEmpId.setEnabled(false);

        javax.swing.GroupLayout pnlDetailEmpLayout = new javax.swing.GroupLayout(pnlDetailEmp);
        pnlDetailEmp.setLayout(pnlDetailEmpLayout);
        pnlDetailEmpLayout.setHorizontalGroup(
            pnlDetailEmpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlDetailEmpLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlDetailEmpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlDetailEmpLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtEmpName1, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtEmpName2, javax.swing.GroupLayout.DEFAULT_SIZE, 96, Short.MAX_VALUE))
                    .addGroup(pnlDetailEmpLayout.createSequentialGroup()
                        .addGroup(pnlDetailEmpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel7)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(pnlDetailEmpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(cmbEmpShop, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtEmpUsr)))
                    .addGroup(pnlDetailEmpLayout.createSequentialGroup()
                        .addGap(11, 11, 11)
                        .addGroup(pnlDetailEmpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlDetailEmpLayout.createSequentialGroup()
                                .addComponent(btnEmpUpdate)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnEmpNew)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnEmpDelete)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(pnlDetailEmpLayout.createSequentialGroup()
                                .addGroup(pnlDetailEmpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel8)
                                    .addComponent(jLabel28))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(pnlDetailEmpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtEmpId)
                                    .addComponent(cmbEmpRank, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))))
                .addContainerGap())
        );
        pnlDetailEmpLayout.setVerticalGroup(
            pnlDetailEmpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlDetailEmpLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlDetailEmpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtEmpName1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtEmpName2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlDetailEmpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(txtEmpUsr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlDetailEmpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(cmbEmpShop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlDetailEmpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbEmpRank, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlDetailEmpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel28)
                    .addComponent(txtEmpId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlDetailEmpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnEmpUpdate)
                    .addComponent(btnEmpNew)
                    .addComponent(btnEmpDelete))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout pnlShopsLayout = new javax.swing.GroupLayout(pnlShops);
        pnlShops.setLayout(pnlShopsLayout);
        pnlShopsLayout.setHorizontalGroup(
            pnlShopsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlShopsLayout.createSequentialGroup()
                .addGroup(pnlShopsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlShopsLayout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(pnlShopsLayout.createSequentialGroup()
                        .addGap(48, 48, 48)
                        .addComponent(jLabel1)
                        .addGap(102, 102, 102)
                        .addComponent(jLabel2)))
                .addGap(10, 10, 10)
                .addGroup(pnlShopsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(pnlDetailsShop, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnlDetailEmp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(47, Short.MAX_VALUE))
        );
        pnlShopsLayout.setVerticalGroup(
            pnlShopsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlShopsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlShopsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlShopsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlShopsLayout.createSequentialGroup()
                        .addComponent(pnlDetailsShop, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(3, 3, 3)
                        .addComponent(pnlDetailEmp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(26, 26, 26))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );

        tbtMain.addTab("Shops & Employees", pnlShops);

        tblProds.setAutoCreateRowSorter(true);
        tblProds.setModel(new javax.swing.table.DefaultTableModel(0, 2));
        tblProds.setAutoscrolls(false);
        tblProds.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane4.setViewportView(tblProds);
        tblProds.setTableHeader(null); //remove header
        //hide first column
        javax.swing.table.TableColumnModel tcm = tblProds.getColumnModel();
        tcm.removeColumn( tcm.getColumn(0) );
        //make table not editable
        tblProds.setDefaultEditor(Object.class, null);

        lstCats.setModel(new javax.swing.DefaultListModel<String>());
        lstCats.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        lstCats.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                lstCatsValueChanged(evt);
            }
        });
        jScrollPane5.setViewportView(lstCats);

        jLabel9.setText("Product Categories");

        jLabel10.setText("Products");

        pnlDetailsCat.setBorder(javax.swing.BorderFactory.createTitledBorder("Category Details"));

        jLabel11.setText("Name");

        jLabel12.setText("ID");

        btnCatUpdate.setText("Update");
        btnCatUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCatUpdateActionPerformed(evt);
            }
        });

        btnCatNew.setText("New");
        btnCatNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCatNewActionPerformed(evt);
            }
        });

        btnCatDelete.setText("Delete");
        btnCatDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCatDeleteActionPerformed(evt);
            }
        });

        txtCatId.setEnabled(false);

        txtCatDesc.setColumns(15);
        txtCatDesc.setRows(4);
        txtCatDesc.setAutoscrolls(false);
        jScrollPane6.setViewportView(txtCatDesc);

        jLabel25.setText("Description");

        javax.swing.GroupLayout pnlDetailsCatLayout = new javax.swing.GroupLayout(pnlDetailsCat);
        pnlDetailsCat.setLayout(pnlDetailsCatLayout);
        pnlDetailsCatLayout.setHorizontalGroup(
            pnlDetailsCatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlDetailsCatLayout.createSequentialGroup()
                .addGroup(pnlDetailsCatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel25)
                    .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel11, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlDetailsCatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtCatId)
                    .addComponent(txtCatName, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
            .addGroup(pnlDetailsCatLayout.createSequentialGroup()
                .addGap(37, 37, 37)
                .addComponent(btnCatUpdate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCatNew)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCatDelete)
                .addContainerGap(37, Short.MAX_VALUE))
        );
        pnlDetailsCatLayout.setVerticalGroup(
            pnlDetailsCatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlDetailsCatLayout.createSequentialGroup()
                .addGroup(pnlDetailsCatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(txtCatName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlDetailsCatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(txtCatId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(pnlDetailsCatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel25))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlDetailsCatLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCatUpdate)
                    .addComponent(btnCatNew)
                    .addComponent(btnCatDelete))
                .addGap(7, 7, 7))
        );

        pnlDetailProd.setBorder(javax.swing.BorderFactory.createTitledBorder("Product Details"));

        btnProdUpdate.setText("Update");
        btnProdUpdate.setEnabled(false);
        btnProdUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnProdUpdateActionPerformed(evt);
            }
        });

        btnProdNew.setText("New");
        btnProdNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnProdNewActionPerformed(evt);
            }
        });

        btnProdDelete.setText("Delete");
        btnProdDelete.setEnabled(false);
        btnProdDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnProdDeleteActionPerformed(evt);
            }
        });

        jLabel13.setText("Name");

        jLabel14.setText("Price");

        jLabel15.setText("ID");

        txtProdId.setEnabled(false);

        txtProdDesc.setColumns(15);
        txtProdDesc.setRows(4);
        txtProdDesc.setAutoscrolls(false);
        jScrollPane7.setViewportView(txtProdDesc);

        jLabel26.setText("Description");

        javax.swing.GroupLayout pnlDetailProdLayout = new javax.swing.GroupLayout(pnlDetailProd);
        pnlDetailProd.setLayout(pnlDetailProdLayout);
        pnlDetailProdLayout.setHorizontalGroup(
            pnlDetailProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlDetailProdLayout.createSequentialGroup()
                .addGroup(pnlDetailProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel26, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(pnlDetailProdLayout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addGroup(pnlDetailProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel15)
                            .addComponent(jLabel14)
                            .addComponent(jLabel13))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(pnlDetailProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtProdId, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jScrollPane7)
                            .addComponent(txtProdPrice)
                            .addComponent(txtProdName))))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlDetailProdLayout.createSequentialGroup()
                .addGap(0, 43, Short.MAX_VALUE)
                .addComponent(btnProdUpdate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnProdNew)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnProdDelete)
                .addGap(31, 31, 31))
        );
        pnlDetailProdLayout.setVerticalGroup(
            pnlDetailProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlDetailProdLayout.createSequentialGroup()
                .addGroup(pnlDetailProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtProdName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13))
                .addGap(4, 4, 4)
                .addGroup(pnlDetailProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtProdPrice, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel14))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlDetailProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(txtProdId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlDetailProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel26))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlDetailProdLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnProdUpdate)
                    .addComponent(btnProdNew)
                    .addComponent(btnProdDelete))
                .addContainerGap())
        );

        javax.swing.GroupLayout pnlProductsLayout = new javax.swing.GroupLayout(pnlProducts);
        pnlProducts.setLayout(pnlProductsLayout);
        pnlProductsLayout.setHorizontalGroup(
            pnlProductsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlProductsLayout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addGroup(pnlProductsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(pnlProductsLayout.createSequentialGroup()
                        .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(19, 19, 19))
                    .addGroup(pnlProductsLayout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel10)
                        .addGap(18, 18, 18)))
                .addGroup(pnlProductsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnlDetailsCat, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnlDetailProd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnlProductsLayout.setVerticalGroup(
            pnlProductsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlProductsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlProductsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlProductsLayout.createSequentialGroup()
                        .addGroup(pnlProductsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel9)
                            .addComponent(jLabel10))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(pnlProductsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane4)
                            .addComponent(jScrollPane5, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addGroup(pnlProductsLayout.createSequentialGroup()
                        .addComponent(pnlDetailsCat, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pnlDetailProd, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        tbtMain.addTab("Products", pnlProducts);

        tblChk.setAutoCreateRowSorter(true);
        tblChk.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        tblChk.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Category", "ID", "Product", "#", "Price"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, true, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblChk.getTableHeader().setReorderingAllowed(false);
        javax.swing.table.TableColumnModel tcmChk = tblChk.getColumnModel();
        tcmChk.getColumn(0).setPreferredWidth(30);
        tcmChk.getColumn(1).setPreferredWidth(200);
        tcmChk.getColumn(2).setPreferredWidth(30);
        tcmChk.getColumn(3).setPreferredWidth(200);
        tcmChk.getColumn(4).setPreferredWidth(35);
        tcmChk.getColumn(5).setPreferredWidth(95);

        tblSP.setViewportView(tblChk);
        if (tblChk.getColumnModel().getColumnCount() > 0) {
            tblChk.getColumnModel().getColumn(0).setResizable(false);
            tblChk.getColumnModel().getColumn(0).setPreferredWidth(50);
            tblChk.getColumnModel().getColumn(1).setResizable(false);
            tblChk.getColumnModel().getColumn(1).setPreferredWidth(250);
            tblChk.getColumnModel().getColumn(2).setResizable(false);
            tblChk.getColumnModel().getColumn(2).setPreferredWidth(50);
        }

        chkInvoice.setText("Invoice");
        chkInvoice.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkInvoiceActionPerformed(evt);
            }
        });

        btnCheckDone.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        btnCheckDone.setText("Done");
        btnCheckDone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCheckDoneActionPerformed(evt);
            }
        });

        pnlInvoice.setBorder(javax.swing.BorderFactory.createTitledBorder("Invoice"));

        jLabel16.setText("Company");
        jLabel16.setEnabled(false);

        cmbInvCname.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "New Client" }));
        cmbInvCname.setEnabled(false);
        cmbInvCname.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbInvCnameActionPerformed(evt);
            }
        });

        txtInvCname.setEnabled(false);

        jLabel17.setText("EIK");
        jLabel17.setEnabled(false);

        txtInvEIK.setEnabled(false);

        jLabel18.setText("First Name");
        jLabel18.setEnabled(false);

        txtInvFname.setEnabled(false);

        jLabel19.setText("Last Name");
        jLabel19.setEnabled(false);

        txtInvLname.setEnabled(false);

        javax.swing.GroupLayout pnlInvoiceLayout = new javax.swing.GroupLayout(pnlInvoice);
        pnlInvoice.setLayout(pnlInvoiceLayout);
        pnlInvoiceLayout.setHorizontalGroup(
            pnlInvoiceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlInvoiceLayout.createSequentialGroup()
                .addGroup(pnlInvoiceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlInvoiceLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(pnlInvoiceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel17)
                            .addComponent(jLabel16))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(pnlInvoiceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtInvCname)
                            .addComponent(cmbInvCname, 0, 0, Short.MAX_VALUE)
                            .addComponent(txtInvEIK)))
                    .addGroup(pnlInvoiceLayout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(pnlInvoiceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel18)
                            .addComponent(jLabel19))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(pnlInvoiceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtInvLname)
                            .addComponent(txtInvFname))))
                .addContainerGap())
        );
        pnlInvoiceLayout.setVerticalGroup(
            pnlInvoiceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlInvoiceLayout.createSequentialGroup()
                .addGroup(pnlInvoiceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlInvoiceLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(cmbInvCname, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtInvCname, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(pnlInvoiceLayout.createSequentialGroup()
                        .addGap(26, 26, 26)
                        .addComponent(jLabel16)))
                .addGap(8, 8, 8)
                .addGroup(pnlInvoiceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(txtInvEIK, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(24, 24, 24)
                .addGroup(pnlInvoiceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(txtInvFname, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlInvoiceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel19)
                    .addComponent(txtInvLname, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pnlChkFilters.setBorder(javax.swing.BorderFactory.createTitledBorder("Filters"));

        jLabel29.setText("Category");

        jLabel31.setText("ID");

        jLabel30.setText("Product");

        jLabel32.setText("Name");

        btnClearCatF.setText("Clear");
        btnClearCatF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearCatFActionPerformed(evt);
            }
        });

        btnClearProdF.setText("Clear");
        btnClearProdF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearProdFActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlChkFiltersLayout = new javax.swing.GroupLayout(pnlChkFilters);
        pnlChkFilters.setLayout(pnlChkFiltersLayout);
        pnlChkFiltersLayout.setHorizontalGroup(
            pnlChkFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlChkFiltersLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlChkFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel29)
                    .addComponent(jLabel30))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlChkFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel31)
                    .addGroup(pnlChkFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(txtFilterProdId)
                        .addComponent(txtFilterCatId, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlChkFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtFilterCatName, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 155, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtFilterProdName, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 155, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel32))
                .addGap(18, 18, 18)
                .addGroup(pnlChkFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnClearCatF)
                    .addComponent(btnClearProdF))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnlChkFiltersLayout.setVerticalGroup(
            pnlChkFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlChkFiltersLayout.createSequentialGroup()
                .addGroup(pnlChkFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel31)
                    .addComponent(jLabel32))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlChkFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtFilterCatName, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtFilterCatId, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel29)
                    .addComponent(btnClearCatF))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlChkFiltersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtFilterProdId, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtFilterProdName, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel30)
                    .addComponent(btnClearProdF))
                .addGap(0, 12, Short.MAX_VALUE))
        );

        jLabel33.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        jLabel33.setText("Current total:");

        lblChkTotal.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        lblChkTotal.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblChkTotal.setText("0.00");

        btnChkClear.setText("Remove all");
        btnChkClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnChkClearActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlCheckoutLayout = new javax.swing.GroupLayout(pnlCheckout);
        pnlCheckout.setLayout(pnlCheckoutLayout);
        pnlCheckoutLayout.setHorizontalGroup(
            pnlCheckoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlCheckoutLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlCheckoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(pnlChkFilters, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tblSP, javax.swing.GroupLayout.PREFERRED_SIZE, 378, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(pnlCheckoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlCheckoutLayout.createSequentialGroup()
                        .addGroup(pnlCheckoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlCheckoutLayout.createSequentialGroup()
                                .addGroup(pnlCheckoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(pnlCheckoutLayout.createSequentialGroup()
                                        .addGap(63, 63, 63)
                                        .addComponent(btnCheckDone, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(pnlCheckoutLayout.createSequentialGroup()
                                        .addGap(18, 18, 18)
                                        .addComponent(jLabel33)))
                                .addGap(0, 55, Short.MAX_VALUE))
                            .addGroup(pnlCheckoutLayout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(pnlCheckoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(pnlCheckoutLayout.createSequentialGroup()
                                        .addComponent(chkInvoice)
                                        .addGap(0, 0, Short.MAX_VALUE))
                                    .addComponent(pnlInvoice, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(lblChkTotal, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                        .addContainerGap())
                    .addGroup(pnlCheckoutLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnChkClear)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        pnlCheckoutLayout.setVerticalGroup(
            pnlCheckoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlCheckoutLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlCheckoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnlChkFilters, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(pnlCheckoutLayout.createSequentialGroup()
                        .addComponent(jLabel33)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblChkTotal)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlCheckoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlCheckoutLayout.createSequentialGroup()
                        .addComponent(btnChkClear)
                        .addGap(6, 6, 6)
                        .addComponent(chkInvoice)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(pnlInvoice, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCheckDone, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(tblSP, javax.swing.GroupLayout.PREFERRED_SIZE, 324, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12))
        );

        tbtMain.addTab("Checkout", pnlCheckout);

        jLabel20.setText("From shop");

        cmbRepShop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbRepShopActionPerformed(evt);
            }
        });

        jLabel21.setText("Created by");

        cmbRepEmp.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel22.setText("From date:");

        txtRepFrom.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel23.setText("to");

        txtRepTo.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        chkRepInv.setText("has Invoice with EIK:");
        chkRepInv.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkRepInvActionPerformed(evt);
            }
        });

        txtRepEIK.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtRepEIK.setText("*");
        txtRepEIK.setEnabled(false);

        bntRepGet.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        bntRepGet.setText("Generate");
        bntRepGet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bntRepGetActionPerformed(evt);
            }
        });

        jLabel24.setText("Get information for all reciepts that are:");

        javax.swing.GroupLayout pnlReportsLayout = new javax.swing.GroupLayout(pnlReports);
        pnlReports.setLayout(pnlReportsLayout);
        pnlReportsLayout.setHorizontalGroup(
            pnlReportsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlReportsLayout.createSequentialGroup()
                .addGroup(pnlReportsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlReportsLayout.createSequentialGroup()
                        .addGap(147, 147, 147)
                        .addGroup(pnlReportsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlReportsLayout.createSequentialGroup()
                                .addComponent(chkRepInv)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(txtRepEIK, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(pnlReportsLayout.createSequentialGroup()
                                .addComponent(jLabel22)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(txtRepFrom, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel23)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtRepTo, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(pnlReportsLayout.createSequentialGroup()
                                .addGroup(pnlReportsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel21)
                                    .addComponent(jLabel20))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(pnlReportsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(cmbRepShop, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(cmbRepEmp, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(jLabel24)))
                    .addGroup(pnlReportsLayout.createSequentialGroup()
                        .addGap(215, 215, 215)
                        .addComponent(bntRepGet, javax.swing.GroupLayout.PREFERRED_SIZE, 148, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(206, Short.MAX_VALUE))
        );
        pnlReportsLayout.setVerticalGroup(
            pnlReportsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlReportsLayout.createSequentialGroup()
                .addGap(61, 61, 61)
                .addComponent(jLabel24)
                .addGap(18, 18, 18)
                .addGroup(pnlReportsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(cmbRepShop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlReportsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel21)
                    .addComponent(cmbRepEmp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(pnlReportsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22)
                    .addComponent(txtRepFrom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel23)
                    .addComponent(txtRepTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(pnlReportsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chkRepInv)
                    .addComponent(txtRepEIK, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bntRepGet, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(182, Short.MAX_VALUE))
        );

        tbtMain.addTab("Report", pnlReports);

        txtUser.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N

        txtPass.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        txtPass.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtPassActionPerformed(evt);
            }
        });

        lblUser.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        lblUser.setText("Username");

        lblPass.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        lblPass.setText("Password");

        btnLogin.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        btnLogin.setText("Log in");
        btnLogin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoginActionPerformed(evt);
            }
        });

        lblPort.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N
        lblPort.setText("Port");

        txtPort.setFont(new java.awt.Font("Tahoma", 0, 14)); // NOI18N
        txtPort.setText("3306");

        javax.swing.GroupLayout pnlLoginLayout = new javax.swing.GroupLayout(pnlLogin);
        pnlLogin.setLayout(pnlLoginLayout);
        pnlLoginLayout.setHorizontalGroup(
            pnlLoginLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlLoginLayout.createSequentialGroup()
                .addGap(160, 160, 160)
                .addGroup(pnlLoginLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlLoginLayout.createSequentialGroup()
                        .addComponent(lblUser)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtUser, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(pnlLoginLayout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(lblPass)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtPass, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(pnlLoginLayout.createSequentialGroup()
                        .addGap(17, 17, 17)
                        .addComponent(lblPort)
                        .addGap(10, 10, 10)
                        .addComponent(txtPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(6, 6, 6)
                        .addComponent(btnLogin, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(256, Short.MAX_VALUE))
        );
        pnlLoginLayout.setVerticalGroup(
            pnlLoginLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlLoginLayout.createSequentialGroup()
                .addGap(138, 138, 138)
                .addGroup(pnlLoginLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtUser, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblUser))
                .addGap(6, 6, 6)
                .addGroup(pnlLoginLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtPass, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPass))
                .addGap(11, 11, 11)
                .addGroup(pnlLoginLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlLoginLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txtPort, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(lblPort))
                    .addGroup(pnlLoginLayout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addComponent(btnLogin, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(236, Short.MAX_VALUE))
        );

        lpMain.setLayer(tbtMain, javax.swing.JLayeredPane.DEFAULT_LAYER);
        lpMain.setLayer(pnlLogin, javax.swing.JLayeredPane.PALETTE_LAYER);

        javax.swing.GroupLayout lpMainLayout = new javax.swing.GroupLayout(lpMain);
        lpMain.setLayout(lpMainLayout);
        lpMainLayout.setHorizontalGroup(
            lpMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tbtMain)
            .addGroup(lpMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(pnlLogin, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        lpMainLayout.setVerticalGroup(
            lpMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tbtMain)
            .addGroup(lpMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(pnlLogin, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        mnuFileGrp.setText("File");

        mnuSaveLog.setText("Save Log");
        mnuSaveLog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuSaveLogActionPerformed(evt);
            }
        });
        mnuFileGrp.add(mnuSaveLog);

        mnuCommit.setText("Force commit to DB");
        mnuCommit.setEnabled(false);
        mnuCommit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuCommitActionPerformed(evt);
            }
        });
        mnuFileGrp.add(mnuCommit);

        mnuExit.setText("Exit");
        mnuExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuExitActionPerformed(evt);
            }
        });
        mnuFileGrp.add(mnuExit);

        jMenuBar1.add(mnuFileGrp);

        mnuUserGrp.setText("User");
        mnuUserGrp.setEnabled(false);

        mnuUinfo.setText("Info");
        mnuUinfo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuUinfoActionPerformed(evt);
            }
        });
        mnuUserGrp.add(mnuUinfo);

        mnuChangePass.setText("Change Password");
        mnuChangePass.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuChangePassActionPerformed(evt);
            }
        });
        mnuUserGrp.add(mnuChangePass);

        mnuLogout.setText("Log out");
        mnuLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuLogoutActionPerformed(evt);
            }
        });
        mnuUserGrp.add(mnuLogout);

        jMenuBar1.add(mnuUserGrp);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addComponent(lpMain))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(lpMain)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    //END GENERATED INIT CODE
    
    //Login pressed
    private void btnLoginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoginActionPerformed
        boolean connectionSuccess = connect(txtUser.getText(), 
                                    String.valueOf(txtPass.getPassword()), 
                                    txtPort.getText());
        
        if (connectionSuccess){
            txtPass.setText("");
            mnuUserGrp.setEnabled(true);
            mnuCommit.setEnabled(true);
            swapLayers(lpMain, pnlLogin, tbtMain);
            loggedIn = true;
            
            
            pnlShopsEnter();
        }
    }//GEN-LAST:event_btnLoginActionPerformed

    private void mnuLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuLogoutActionPerformed
        mnuUserGrp.setEnabled(false);
        mnuCommit.setEnabled(false);
        exitProcedure();
        swapLayers(lpMain, pnlLogin, tbtMain);
        loggedIn = false;
        System.out.println("logged out");
    }//GEN-LAST:event_mnuLogoutActionPerformed

    private void mnuCommitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuCommitActionPerformed
        //dbWrite.setForceCommit(true);
        queryStrQueue.add("fc");
    }//GEN-LAST:event_mnuCommitActionPerformed

    private void chkInvoiceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkInvoiceActionPerformed
        setEnabledPanel(pnlInvoice, chkInvoice.isSelected());
        cmbInvCname.setSelectedIndex(0);
    }//GEN-LAST:event_chkInvoiceActionPerformed

    private void chkRepInvActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkRepInvActionPerformed
        txtRepEIK.setEnabled(chkRepInv.isSelected());
    }//GEN-LAST:event_chkRepInvActionPerformed

    private void mnuSaveLogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuSaveLogActionPerformed
        //TODO add code
    }//GEN-LAST:event_mnuSaveLogActionPerformed

    
    private void tblProdsListSelection(ListSelectionEvent evt){
        if (!evt.getValueIsAdjusting()){
            int selected = tblProdsSM.getAnchorSelectionIndex();
            
            if (selected == 0 || selected == -1){ //-1 = nothing selected
                clearTxts(pnlDetailProd);
                btnProdUpdate.setEnabled(false);
                btnProdDelete.setEnabled(false); 
                btnProdNew.setEnabled((selected == 0));
            } else {          
                int absoluteProdIndex = tblProdsRS.convertRowIndexToModel(selected);
                loadProduct(products.get(absoluteProdIndex - 1));
                
                if (evt.getFirstIndex() == 0){ //these were disabled previously
                    btnProdUpdate.setEnabled(true);
                    btnProdDelete.setEnabled(true);
                    btnProdNew.setEnabled(true);
                }
            }       
        }
    }
    
    private void tblEmpsListSelection(ListSelectionEvent evt){
        if (!evt.getValueIsAdjusting()){
            int selected = tblEmpsSM.getAnchorSelectionIndex();
            
            if (selected == 0 || selected == -1){ //-1 = nothing selected
                clearTxts(pnlDetailEmp);
                
                if(usrRank > 0){
                    btnEmpUpdate.setEnabled(false);
                    btnEmpDelete.setEnabled(false); 
                    btnEmpNew.setEnabled((selected == 0));

                    setTxtEnabled(pnlDetailEmp, true);
                    txtEmpId.setEnabled(false);
                }
            } else {          
                int absoluteEmpIndex = tblEmpsRS.convertRowIndexToModel(selected);
                loadEmp(employees.get(absoluteEmpIndex - 1));
                
                if (evt.getFirstIndex() == 0 && usrRank > 0){ //these were disabled previously
                    btnEmpUpdate.setEnabled(true);
                    btnEmpDelete.setEnabled(true);
                    btnEmpNew.setEnabled(false);
                    
                    setTxtEnabled(pnlDetailEmp, false);
                }
            }       
        }
    }
    
    /**
     * calculate total price at checkout at products added/removed
     * @param tcl 
     */
    private void calcPrice(TableCellListener tcl){
        int newV = (int) tcl.getNewValue();
        int oldV = (int) tcl.getOldValue();
        if (newV >= 0){
            double price = (double) tblChkM.getValueAt(tcl.getRow(), tcl.getColumn()+1);
            chkTotal += (newV - oldV) * price;
            lblChkTotal.setText(String.format("%.2f", chkTotal));
        } else {
            tblChkM.setValueAt(oldV, tcl.getRow(), tcl.getColumn());
            System.out.println("Negative value entered for # products bought.. reseting");
        }
    }
    
    /**
     * when tabs are changed call the appropriate manager functions
     * @param evt 
     */
    private void tbtMainStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tbtMainStateChanged
        if(loggedIn){
           //leaving panel
           switch(focusedTab){
               case PNL_SHOPS: pnlShopsExit(); break;
               case PNL_PRODUCTS: pnlProductsExit(); break;
               case PNL_CHECKOUT: pnlCheckoutExit(); break;
               case PNL_REPORTS: pnlReportsExit(); break;
           }
           
           //entering panel
           focusedTab = tbtMain.getSelectedIndex();
           switch(focusedTab){
               case PNL_SHOPS: pnlShopsEnter(); break;
               case PNL_PRODUCTS: pnlProductsEnter(); break;
               case PNL_CHECKOUT: pnlCheckoutEnter(); break;
               case PNL_REPORTS: pnlReportsEnter(); break;
           }
        }
    }//GEN-LAST:event_tbtMainStateChanged

    private void cmbInvCnameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbInvCnameActionPerformed
        int selected =  cmbInvCname.getSelectedIndex();
        if (selected > 0){ //selected existing company
            loadClient(clients.get(selected-1));
        } else { //selected 'new company'
            clearTxts(pnlInvoice);
        }
    }//GEN-LAST:event_cmbInvCnameActionPerformed

    private void btnCheckDoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCheckDoneActionPerformed
        if(chkTotal > 0){
            String timestamp = LocalDateTime.now().format(DBtimeFormat);
            //Integer invId = null;
            Client tmpClient = null;
            Invoice inv = null;
              
            //invoice checked
            if (chkInvoice.isSelected()){
                //clients
                int selected =  cmbInvCname.getSelectedIndex();
                

                if (selected == 0){ //add new client
                    int newClientKey;

                    tmpClient = new Client(txtInvEIK.getText(), 
                            txtInvFname.getText(), 
                            txtInvLname.getText(), 
                            txtInvCname.getText());

                    //tmpClient.setNewInstance(true); - inserting immediately
                    clients.add(tmpClient);

                    newClientKey = clientRepository.InsertGetKey(tmpClient);
                    tmpClient.clientId = newClientKey;

                    cmbInvCname.addItem(newClientKey + ": " + tmpClient.companyName +" - "+ tmpClient.firstname);
                } else { //update existing client
                    tmpClient = clients.get(selected - 1); //-1 since first item is 'new'
                    tmpClient.eik = txtInvEIK.getText();
                    tmpClient.firstname = txtInvFname.getText();
                    tmpClient.lastname = txtInvLname.getText();
                    tmpClient.companyName = txtInvCname.getText();
                    tmpClient.setUpdated(true);
                }
                //invoice
                inv = new Invoice(tmpClient.clientId, timestamp);
                inv.invoiceId = invoiceRepository.InsertGetKey(inv);
            }
            
            //receipt
            ArrayList<BoughtProduct> boughtProds = new ArrayList<>();
            int tmpBoughtN;
            BoughtProduct tmpBoughtProd;
            Integer invId = (inv == null)? null : inv.invoiceId;
            
            Receipt recp = new Receipt( (chkInvoice.isSelected())? 1 : 0,
                                        invId,
                                        me.employeeId,
                                        timestamp);
            recp.receiptId = receiptRepository.InsertGetKey(recp);
            
            
            for (int i = 0; i < tblChkM.getRowCount(); i++){
                tmpBoughtN = (int) tblChkM.getValueAt(i, 4);
                //System.out.println(i + " " + tmpBoughtN);
                if (tmpBoughtN > 0){
                    
                    tmpBoughtProd = new BoughtProduct((int)tblChkM.getValueAt(i, 2),
                                                        recp.receiptId,
                                                        tmpBoughtN,
                                                        (double)tblChkM.getValueAt(i, 5));
                    boughtRepository.Insert(tmpBoughtProd);
                    boughtProds.add(tmpBoughtProd);
                }
            }
            Shop s = shopsDict.get(me.shopId);
            String recptH = "Receipt #" + recp.receiptId;
            String empH = "Agent " + me.firstname +" "+  me.lastname;
            String empH2 = "Employee " + me.employeeId;
            Dialogues.showReceipt(new String[] {recptH,
                                            s.shopName, 
                                            s.address, 
                                            recp.buyDate,
                                            empH,
                                            empH2}, 
                                boughtProds,
                                prodNames,
                                chkTotal);
            
            //resest UI
            if (chkInvoice.isSelected()){
                //show invoice
                String invH = "Invoice #"+recp.invoiceId;
                String invHr = "For receipt #"+recp.receiptId;
                Dialogues.showInvoice(new String[] {s.shopName,invH,invHr}, tmpClient, recp, chkTotal);
                
                chkInvoice.doClick();
                clearTxts(pnlInvoice);
            }
            btnChkClear.doClick();
            btnClearCatF.doClick();
            btnClearProdF.doClick();
            
        } else {
            System.out.println("Nothing bought");
        }
    }//GEN-LAST:event_btnCheckDoneActionPerformed

    private void lstCatsValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstCatsValueChanged
        //System.err.println(evt.toString());
        //category details
        if(!evt.getValueIsAdjusting()){ //list is updated
            int selected = lstCats.getSelectedIndex();
            
            if (selected == 0 || selected == -1){ //new category
                clearTxts(pnlDetailsCat);
                //tblProds.setEnabled(false);
                //remove all items from product table
                setRowFilterInt(tblProdsRS, new int [] {-2}, 0);
                btnCatUpdate.setEnabled(false);
                btnCatDelete.setEnabled(false);
                setTxtEnabled(pnlDetailProd, false);
            } else {
                Category selectedCat = categories.get(selected - 1);
                loadCategory(selectedCat);
                
                //-1 is the value for "new"
                setRowFilterInt(tblProdsRS, new int [] {selectedCat.productCategoriyId, -1}, 0);
                
                if (evt.getFirstIndex() == 0 || evt.getFirstIndex() == evt.getLastIndex()){ //if last index = first index -> something selected for the first time
                    //everything in product details was disabled
                    setTxtEnabled(pnlDetailProd, true); 
                    txtProdId.setEnabled(false);
                    //tblProds.setEnabled(true);
                    btnCatUpdate.setEnabled(true);
                    btnCatDelete.setEnabled(true);
                }
            
                tblProds.setRowSelectionInterval(0, 0);
            }
        }
    }//GEN-LAST:event_lstCatsValueChanged

    private void btnCatUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCatUpdateActionPerformed
        if (Dialogues.confirmYesNo(Dialogues.OVERWRITE_TXT)){
            Category c = categories.get(lstCats.getSelectedIndex()-1);
            c.categoryName = txtCatName.getText();
            c.description = txtCatDesc.getText();
            c.setUpdated(true);
        }
    }//GEN-LAST:event_btnCatUpdateActionPerformed

    private void btnCatNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCatNewActionPerformed
        Category c = new Category (txtCatName.getText(), txtCatDesc.getText());
        //need key immediately in case we want to add products
        c.productCategoriyId = catRepository.InsertGetKey(c);
        categories.add(c);
        lstCatsM.addElement(Integer.toString(c.productCategoriyId) +" - "+ c.categoryName);
    }//GEN-LAST:event_btnCatNewActionPerformed

    private void btnCatDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCatDeleteActionPerformed
        if (tblProds.getRowCount() > 1) { //getRowCount returns visible rows only
            System.out.println("Category is not empty, delete all products if you want to remove it");
        } else if (Dialogues.confirmYesNo(Dialogues.DELETE_TXT)){
            int selected = lstCats.getSelectedIndex();
            //System.out.println(selected);
            Category c = categories.get(selected-1);
            if (c.isNewInstance()){
                categories.remove(selected-1);
            } else {
                c.active = 0;
                c.setUpdated(true);
            }
            
            lstCats.setSelectedIndex(selected-1); //prevent exception from removing selection
            lstCatsM.removeElementAt(selected);
        }
    }//GEN-LAST:event_btnCatDeleteActionPerformed

    private void btnProdUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnProdUpdateActionPerformed
        if (Dialogues.confirmYesNo(Dialogues.OVERWRITE_TXT)){
            int selected = tblProdsSM.getAnchorSelectionIndex();
            //first row is "new"
            int absoluteProdIndex = tblProdsRS.convertRowIndexToModel(selected)-1;
            Product p = products.get(absoluteProdIndex);
            
            p.productName = txtProdName.getText();
            p.price =  Double.parseDouble(txtProdPrice.getText());
            p.description = txtProdDesc.getText();
            
            p.setUpdated(true);
        }
    }//GEN-LAST:event_btnProdUpdateActionPerformed

    private void btnProdNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnProdNewActionPerformed
        int catId = categories.get(lstCats.getSelectedIndex()-1).productCategoriyId;
        Product p = new Product(txtProdName.getText(), 
                            Double.parseDouble(txtProdPrice.getText()), 
                            catId, 
                            txtProdDesc.getText());
        p.productId = -1;
        
        p.setNewInstance(true);
        products.add(p);
        
        tblProdsM.addRow(new Object[] {catId, "NA - "+ p.productName});
    }//GEN-LAST:event_btnProdNewActionPerformed

    private void btnProdDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnProdDeleteActionPerformed
        if (Dialogues.confirmYesNo(Dialogues.DELETE_TXT)){
            int selected = tblProdsSM.getAnchorSelectionIndex();
            int absoluteProdIndex = tblProdsRS.convertRowIndexToModel(selected);

            Product p = products.get(absoluteProdIndex-1);
            if (p.isNewInstance()){ //new object not yet added to db
                products.remove(absoluteProdIndex - 1);
            } else {
                p.active = 0;
                p.setUpdated(true);
            }
            
            tblProdsSM.setSelectionInterval(selected-1, selected-1);
            tblProdsM.removeRow(absoluteProdIndex);
        }
    }//GEN-LAST:event_btnProdDeleteActionPerformed

    private void lstShopsValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstShopsValueChanged
        if(!evt.getValueIsAdjusting()){ //list is updated
            int selected = lstShops.getSelectedIndex();
            
            if (selected == 0 || selected == -1){ //new category
                clearTxts(pnlDetailsShop);
                //remove all items from product table
                setRowFilterInt(tblEmpsRS, new int [] {-2}, 0);
                btnShopUpdate.setEnabled(false);
                btnShopDelete.setEnabled(false);
                setTxtEnabled(pnlDetailEmp, false);
                //disable employee combo boxes
                cmbEmpRank.setEnabled(false);
                cmbEmpShop.setEnabled(false);
            } else {
                Shop selectedShop = shops.get(selected - 1);
                loadShop(selectedShop);
                
                //-1 is the value for "new"
                setRowFilterInt(tblEmpsRS, new int [] {selectedShop.shopId, -1}, 0);
                
                if (evt.getFirstIndex() == 0 || evt.getFirstIndex() == evt.getLastIndex()){ //if last index = first index -> something selected for the first time
                    btnShopUpdate.setEnabled(true);
                    btnShopDelete.setEnabled(true);
                    if(usrRank > 0){
                        cmbEmpRank.setEnabled(true);
                        cmbEmpShop.setEnabled(true);
                    }
                }
            
                tblEmps.setRowSelectionInterval(0, 0);
            }
        }
    }//GEN-LAST:event_lstShopsValueChanged

    private void btnShopUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnShopUpdateActionPerformed
        if (Dialogues.confirmYesNo(Dialogues.OVERWRITE_TXT)){
            Shop s = shops.get(lstShops.getSelectedIndex()-1);
            s.shopName = txtShopName.getText();
            s.address = txtShopAdress.getText();
            s.setUpdated(true);
        }
    }//GEN-LAST:event_btnShopUpdateActionPerformed

    private void btnShopNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnShopNewActionPerformed
        Shop s = new Shop(txtShopName.getText(), txtShopAdress.getText());
        s.shopId = shopRepository.InsertGetKey(s);
        shops.add(s);
        lstShopsM.addElement(Integer.toString(s.shopId) +" - "+ s.shopName);
        cmbEmpShop.addItem(Integer.toString(s.shopId)+" - "+s.shopName);
    }//GEN-LAST:event_btnShopNewActionPerformed

    private void btnShopDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnShopDeleteActionPerformed
        if (tblEmps.getRowCount() > 1) { //getRowCount returns visible rows only
            System.out.println("Shop has employees, delete all employees if you want to remove it");
        } else if (Dialogues.confirmYesNo(Dialogues.DELETE_TXT)){
            int selected = lstShops.getSelectedIndex();
            //System.out.println(selected);
            Shop s = shops.get(selected-1);
            if (s.isNewInstance()){
                shops.remove(selected-1);
            } else {
                s.active = 0;
                s.setUpdated(true);
            }
            
            lstShops.setSelectedIndex(selected-1); //prevent exception from removing selection
            lstShopsM.removeElementAt(selected);
        }
    }//GEN-LAST:event_btnShopDeleteActionPerformed

    private void btnEmpUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEmpUpdateActionPerformed
        if (Dialogues.confirmYesNo(Dialogues.OVERWRITE_TXT)){
            int selected = tblEmpsSM.getAnchorSelectionIndex();
            //first row is "new"
            int absoluteEmpIndex = tblEmpsRS.convertRowIndexToModel(selected)-1;
            Employee e = employees.get(absoluteEmpIndex);
            int accessLvlNew = cmbEmpRank.getSelectedIndex();
            //if access level is change we need to change priviliges
            if (e.accessLvl != accessLvlNew){
                if(e.accessLvl > accessLvlNew){
                    queryStrQueue.offer(DBwriteThread.getRevokeStr(e.username, e.accessLvl));
                } else {
                    queryStrQueue.offer(DBwriteThread.getGrantStr(e.username, accessLvlNew));
                }
                e.accessLvl = cmbEmpRank.getSelectedIndex();
            }
            e.shopId = shops.get(cmbEmpShop.getSelectedIndex()).shopId;
            
            e.setUpdated(true);
        }
    }//GEN-LAST:event_btnEmpUpdateActionPerformed

    private void btnEmpNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEmpNewActionPerformed
        String pwd = Dialogues.passwordNew();
        if (pwd != null){
            int shopId = shops.get(lstShops.getSelectedIndex()-1).shopId;
            Employee e = new Employee(txtEmpUsr.getText(), 
                                txtEmpName1.getText(),
                                txtEmpName2.getText(),
                                cmbEmpRank.getSelectedIndex(),                           
                                shopId);
            e.employeeId = -1;

            e.setNewInstance(true);
            employees.add(e);

            tblEmpsM.addRow(new Object[] {shopId, "NA - "+ e.firstname});
            queryStrQueue.offer(DBwriteThread.getNewUserStr(e.username, pwd));
            for (int i = 0; i <= e.accessLvl; i++) { //add all roles up to the selected one as well
                queryStrQueue.offer(DBwriteThread.getGrantStr(e.username, i));
            } 
        }
    }//GEN-LAST:event_btnEmpNewActionPerformed

    private void btnEmpDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEmpDeleteActionPerformed
        if (Dialogues.confirmYesNo(Dialogues.DELETE_TXT)){
            int selected = tblEmpsSM.getAnchorSelectionIndex();
            int absoluteEmpIndex = tblEmpsRS.convertRowIndexToModel(selected);

            Employee e = employees.get(absoluteEmpIndex-1);
            if (e.isNewInstance()){ //new object not yet added to db
                employees.remove(absoluteEmpIndex - 1);
            } else {
                e.active = 0;
                e.setUpdated(true);
            }
            
            tblEmpsSM.setSelectionInterval(selected-1, selected-1);
            tblEmpsM.removeRow(absoluteEmpIndex);
            queryStrQueue.offer(DBwriteThread.getDropUserStr(e.username));
        }
    }//GEN-LAST:event_btnEmpDeleteActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        exitProcedure();
        this.dispose();
        System.exit(0);
    }//GEN-LAST:event_formWindowClosing

    private void mnuExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuExitActionPerformed
        formWindowClosing(null);
    }//GEN-LAST:event_mnuExitActionPerformed

    private void txtPassActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPassActionPerformed
        btnLogin.doClick();
    }//GEN-LAST:event_txtPassActionPerformed

    private void btnClearCatFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearCatFActionPerformed
        txtFilterCatId.setText("");
        txtFilterCatName.setText("");
    }//GEN-LAST:event_btnClearCatFActionPerformed

    private void btnClearProdFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearProdFActionPerformed
        txtFilterProdId.setText("");
        txtFilterProdName.setText("");
    }//GEN-LAST:event_btnClearProdFActionPerformed

    private void btnChkClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnChkClearActionPerformed
        for (int i = 0; i < tblChkM.getRowCount(); i++){
            tblChkM.setValueAt(0, i, 4); 
        }
        chkTotal = 0.00;
        lblChkTotal.setText("0.00");
    }//GEN-LAST:event_btnChkClearActionPerformed

    private void mnuChangePassActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuChangePassActionPerformed
        String pw;
        if ((pw = Dialogues.passwordNew()) != null){
            queryStrQueue.offer(DBwriteThread.getSetPasswordStr(me.username, pw));
        }
    }//GEN-LAST:event_mnuChangePassActionPerformed

    private void mnuUinfoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuUinfoActionPerformed
        Dialogues.showEmpInfo(me);
    }//GEN-LAST:event_mnuUinfoActionPerformed

    private void cmbRepShopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbRepShopActionPerformed
        int selected = cmbRepShop.getSelectedIndex();
        cmbRepEmp.addItem("All");

    }//GEN-LAST:event_cmbRepShopActionPerformed

    private void bntRepGetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bntRepGetActionPerformed
        try {
            Report rep = new Report(DBconnection, null);
            ArrayList <String> res;
            res = rep.WriteReportReceipts(me, new Date(1999,0, 0),new Date(2200,0, 0));
            
            for (String s: res){
                System.out.println(res);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
    }//GEN-LAST:event_bntRepGetActionPerformed

    
    
    //<editor-fold defaultstate="collapsed" desc="autogenreated variables">
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bntRepGet;
    private javax.swing.JButton btnCatDelete;
    private javax.swing.JButton btnCatNew;
    private javax.swing.JButton btnCatUpdate;
    private javax.swing.JButton btnCheckDone;
    private javax.swing.JButton btnChkClear;
    private javax.swing.JButton btnClearCatF;
    private javax.swing.JButton btnClearProdF;
    private javax.swing.JButton btnEmpDelete;
    private javax.swing.JButton btnEmpNew;
    private javax.swing.JButton btnEmpUpdate;
    private javax.swing.JButton btnLogin;
    private javax.swing.JButton btnProdDelete;
    private javax.swing.JButton btnProdNew;
    private javax.swing.JButton btnProdUpdate;
    private javax.swing.JButton btnShopDelete;
    private javax.swing.JButton btnShopNew;
    private javax.swing.JButton btnShopUpdate;
    private javax.swing.JCheckBox chkInvoice;
    private javax.swing.JCheckBox chkRepInv;
    private javax.swing.JComboBox<String> cmbEmpRank;
    private javax.swing.JComboBox<String> cmbEmpShop;
    private javax.swing.JComboBox<String> cmbInvCname;
    private javax.swing.JComboBox<String> cmbRepEmp;
    private javax.swing.JComboBox<String> cmbRepShop;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JLabel lblChkTotal;
    private javax.swing.JLabel lblPass;
    private javax.swing.JLabel lblPort;
    private javax.swing.JLabel lblUser;
    private javax.swing.JLayeredPane lpMain;
    private javax.swing.JList<String> lstCats;
    private javax.swing.JList<String> lstShops;
    private javax.swing.JMenuItem mnuChangePass;
    private javax.swing.JMenuItem mnuCommit;
    private javax.swing.JMenuItem mnuExit;
    private javax.swing.JMenu mnuFileGrp;
    private javax.swing.JMenuItem mnuLogout;
    private javax.swing.JMenuItem mnuSaveLog;
    private javax.swing.JMenuItem mnuUinfo;
    private javax.swing.JMenu mnuUserGrp;
    private javax.swing.JPanel pnlCheckout;
    private javax.swing.JPanel pnlChkFilters;
    private javax.swing.JPanel pnlDetailEmp;
    private javax.swing.JPanel pnlDetailProd;
    private javax.swing.JPanel pnlDetailsCat;
    private javax.swing.JPanel pnlDetailsShop;
    private javax.swing.JPanel pnlInvoice;
    private javax.swing.JPanel pnlLogin;
    private javax.swing.JPanel pnlProducts;
    private javax.swing.JPanel pnlReports;
    private javax.swing.JPanel pnlShops;
    private javax.swing.JTable tblChk;
    private javax.swing.JTable tblEmps;
    private javax.swing.JTable tblProds;
    private javax.swing.JScrollPane tblSP;
    private javax.swing.JTabbedPane tbtMain;
    private javax.swing.JTextArea txtCatDesc;
    private javax.swing.JTextField txtCatId;
    private javax.swing.JTextField txtCatName;
    private javax.swing.JTextField txtEmpId;
    private javax.swing.JTextField txtEmpName1;
    private javax.swing.JTextField txtEmpName2;
    private javax.swing.JTextField txtEmpUsr;
    private javax.swing.JTextField txtFilterCatId;
    private javax.swing.JTextField txtFilterCatName;
    private javax.swing.JTextField txtFilterProdId;
    private javax.swing.JTextField txtFilterProdName;
    private javax.swing.JTextField txtInvCname;
    private javax.swing.JTextField txtInvEIK;
    private javax.swing.JTextField txtInvFname;
    private javax.swing.JTextField txtInvLname;
    private javax.swing.JTextArea txtLog;
    private javax.swing.JPasswordField txtPass;
    private javax.swing.JTextField txtPort;
    private javax.swing.JTextArea txtProdDesc;
    private javax.swing.JTextField txtProdId;
    private javax.swing.JTextField txtProdName;
    private javax.swing.JTextField txtProdPrice;
    private javax.swing.JTextField txtRepEIK;
    private javax.swing.JTextField txtRepFrom;
    private javax.swing.JTextField txtRepTo;
    private javax.swing.JTextField txtShopAdress;
    private javax.swing.JTextField txtShopId;
    private javax.swing.JTextField txtShopName;
    private javax.swing.JTextField txtUser;
    // End of variables declaration//GEN-END:variables

    private void pnlReportsExit() {
    }
    //</editor-fold>


}
