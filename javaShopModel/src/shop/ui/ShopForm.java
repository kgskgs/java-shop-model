/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.ui;

import java.awt.Component;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.RowFilter.ComparisonType;
import javax.swing.RowSorter;
import javax.swing.SingleSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import shop.db.*;
import shop.infrastructure.MySqlRepository;
import shop.models.*;

/**
 * 
 * @author K1
 */
public class ShopForm extends javax.swing.JFrame implements Runnable {

    
    public static final int PNL_SHOPS = 0;
    public static final int PNL_PRODUCTS = 1;
    public static final int PNL_CHECKOUT = 2;

    //db custom vars declaration
    private final BlockingQueue<String> queryStrQueue;
    private Connection DBconnection;
    DBwriteThread dbWrite;
    
    private DateTimeFormatter DBtimeFormat;
    
    //internal flags
    private boolean loggedIn = false;
    private int focusedTab = 0;
    
    //model entities
    //invoice
    ArrayList<Client> clients;
    MySqlRepository<Client> clientRepository;
    MySqlRepository<Invoice> invoiceRepository;
    
    //products
    ArrayList<Category> categories;
    ArrayList<Product> products;
    MySqlRepository<Category> catRepository;
    MySqlRepository<Product> prodRepository;
    //ui representations of models
    DefaultListModel<String> lstCatsM;
    //DefaultListModel<String> lstProdsM;
    //ListModelManager<Product> prodLstManager;
    DefaultTableModel tblProdsM;
    DefaultListSelectionModel tblProdsSM;
    TableRowSorter<? extends TableModel>  tblProdsRS;
    
 
    /**
     * Creates new form ShopForm, redirects os to doc
     */  
    public ShopForm() {
        //run generated code
        initComponents();
        //get needed vars from generated code
        //this.lstProdsM = (DefaultListModel<String>) lstProds.getModel();
        this.lstCatsM = (DefaultListModel<String>) lstCats.getModel();
        tblProdsM = (DefaultTableModel) tblProds.getModel();
        tblProdsSM = (DefaultListSelectionModel) tblProds.getSelectionModel();
        //table list selection can't be added from netbeans gui
        tblProdsSM.addListSelectionListener((ListSelectionEvent e) -> {
            tblProdsListSelection(e);
        });
        
        tblProdsRS = (TableRowSorter) tblProds.getRowSorter(); 

        
        //set up log textarea
        PrintStream os = new PrintStream(new LogDocStream(txtLog.getDocument()), true);
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
            System.out.print("connected to database as " + usr);
            
            dbWrite = new DBwriteThread(DBconnection, queryStrQueue);
            dbWrite.start();
            
            clientRepository = new MySqlRepository<>(Client.class, DBconnection, queryStrQueue);
            invoiceRepository = new MySqlRepository<>(Invoice.class, DBconnection, queryStrQueue);
            catRepository = new MySqlRepository<>(Category.class, DBconnection, queryStrQueue);
            prodRepository = new MySqlRepository<>(Product.class, DBconnection, queryStrQueue);
            
            return true;
        } catch (ClassNotFoundException | SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    
    //ENTITY OPERATIONS
    public void pnlShopsEnter(){
    
    }
    
    public void pnlShopsExit(){
    
    }
    
    public void pnlProductsEnter(){
        try {
            lstCatsM.addElement("New Category");
            //lstProdsM.addElement("New");
            
            //ArrayList<Integer> catKeys = new ArrayList<>();
            
            categories = catRepository.GetAll(0, 1000, false);
            products = prodRepository.GetAll(0, 1000, true);
            //Collections.sort(products, new ProdSortByCategory());
            
            for (Category c : categories) {
                lstCatsM.addElement(c.productCategoriyId+" - " + c.categoryName);
                //catKeys.add(c.productCategoriyId);
            }
            
            tblProdsM.addRow(new Object[] {-1, "New Product"});
            
            for (Product p : products){
                tblProdsM.addRow(new Object[] {p.categoryId, Integer.toString(p.productId) +" - "+ p.productName});
            }
            
            //prodLstManager = new ListModelManager<> (Product.class, products, catKeys);
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    public void pnlProductsExit(){
    
    }
    
    public void pnlCheckoutEnter(){   
        //load invoice panel
        try {
            clients = clientRepository.GetAll(0, 1000, false);

            for (Client c: clients){
                cmbInvCname.addItem(c.clientId+": " +c.companyName +" - "+ c.firstname);
            }  
            
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * run when tab switched away from 'checkout'
     * empty used variables and reset ui
     */
    public void pnlCheckoutExit(){
        //Invoice panel
        //ui
        if(chkInvoice.isSelected())
            chkInvoice.doClick(); //triggers the action event listener
        //reset combobox
        cmbInvCname.removeAllItems();
        cmbInvCname.addItem("New Client");
        
        //update entities
        for (Client c : clients) {
            if (c.isNewInstance())
                clientRepository.Insert(c);
            else if (c.isUpdated())
                clientRepository.Update(c);
        }
        //we'll be reloading these try to free memory while not in use
        clients.clear();
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
    
    private void loadCategory(Category c){
        txtCatName.setText(c.categoryName);
        txtCatId.setText(Integer.toString(c.productCategoriyId));
        txtCatDesc.setText(c.description);
    }
    
    private void loadProduct(Product p){
        txtProdName.setText(p.productName);
        txtProdPrice.setText(String.valueOf(p.price));
        txtProdDesc.setText(p.description);
        txtProdId.setText(Integer.toString(p.productId));
    }
    //UI HELPER FUNCTIONS
    /**
     * swap two components in the layered pane
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
           //System.out.println(c.toString());
           if (c instanceof JTextField){
               ((JTextField) c).setText("");
           } else if (c instanceof JScrollPane){
               //System.out.println("test");
               Component c1 = ((JScrollPane)c).getViewport().getComponent(0);
               ((JTextArea) (c1)).setText("");
           }
       }
    }
    
    
    private void setRowFilterInt(TableRowSorter<? extends TableModel> rs, int[] vals, int colIndex){
        ArrayList<RowFilter<Object,Object>> allFilters = new ArrayList<>();
        for (int i = 0; i < vals.length; i++){
            allFilters.add(RowFilter.numberFilter(ComparisonType.EQUAL, vals[i], colIndex));
        }
        rs.setRowFilter(RowFilter.orFilter(allFilters));
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
        lstShops = new javax.swing.JList<>();
        jScrollPane3 = new javax.swing.JScrollPane();
        lstEmps = new javax.swing.JList<>();
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
        pnlDetailEmp = new javax.swing.JPanel();
        btnEmpUpdate = new javax.swing.JButton();
        btnEmpNew = new javax.swing.JButton();
        btnEmpDelete = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        txtEmpName1 = new javax.swing.JTextField();
        txtEmpName2 = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        txtEmpKey = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        cmbEmpShop = new javax.swing.JComboBox<>();
        jLabel8 = new javax.swing.JLabel();
        cmbEmpRank = new javax.swing.JComboBox<>();
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
        tblCheckProds = new javax.swing.JTable();
        cmbCheckProd = new javax.swing.JComboBox<>();
        btnCheckAdd = new javax.swing.JButton();
        chkInvoice = new javax.swing.JCheckBox();
        btnCheckDel = new javax.swing.JButton();
        txtCheckNum = new javax.swing.JTextField();
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
        txtCheckProdId = new javax.swing.JTextField();
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
        mnuLogout = new javax.swing.JMenuItem();
        mnuUinfo = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setFocusCycleRoot(false);
        setResizable(false);

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

        lstShops.setModel(new javax.swing.DefaultListModel<String>());
        lstShops.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(lstShops);

        lstEmps.setModel(new javax.swing.DefaultListModel<String>());
        lstEmps.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane3.setViewportView(lstEmps);

        jLabel1.setText("Shops");

        jLabel2.setText("Employees");

        pnlDetailsShop.setBorder(javax.swing.BorderFactory.createTitledBorder("Shop Details"));

        jLabel3.setText("Name");

        jLabel4.setText("Adress");

        btnShopUpdate.setText("Update");

        btnShopNew.setText("New");

        btnShopDelete.setText("Delete");

        javax.swing.GroupLayout pnlDetailsShopLayout = new javax.swing.GroupLayout(pnlDetailsShop);
        pnlDetailsShop.setLayout(pnlDetailsShopLayout);
        pnlDetailsShopLayout.setHorizontalGroup(
            pnlDetailsShopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlDetailsShopLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlDetailsShopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlDetailsShopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtShopAdress)
                    .addComponent(txtShopName))
                .addContainerGap())
            .addGroup(pnlDetailsShopLayout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(btnShopUpdate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnShopNew)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnShopDelete)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        pnlDetailsShopLayout.setVerticalGroup(
            pnlDetailsShopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlDetailsShopLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(pnlDetailsShopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtShopName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlDetailsShopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(txtShopAdress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlDetailsShopLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnShopUpdate)
                    .addComponent(btnShopNew)
                    .addComponent(btnShopDelete))
                .addGap(7, 7, 7))
        );

        pnlDetailEmp.setBorder(javax.swing.BorderFactory.createTitledBorder("Employee Details"));

        btnEmpUpdate.setText("Update");

        btnEmpNew.setText("New");

        btnEmpDelete.setText("Delete");

        jLabel5.setText("Name");

        jLabel7.setText("Username");

        jLabel6.setText("Shop");

        cmbEmpShop.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel8.setText("Position");

        cmbEmpRank.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout pnlDetailEmpLayout = new javax.swing.GroupLayout(pnlDetailEmp);
        pnlDetailEmp.setLayout(pnlDetailEmpLayout);
        pnlDetailEmpLayout.setHorizontalGroup(
            pnlDetailEmpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlDetailEmpLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlDetailEmpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlDetailEmpLayout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(pnlDetailEmpLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtEmpName1, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtEmpName2))
                    .addGroup(pnlDetailEmpLayout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(pnlDetailEmpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(cmbEmpShop, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtEmpKey)))
                    .addGroup(pnlDetailEmpLayout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addGap(15, 15, 15)
                        .addComponent(cmbEmpRank, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(pnlDetailEmpLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(btnEmpUpdate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnEmpNew)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnEmpDelete)
                .addContainerGap(30, Short.MAX_VALUE))
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
                    .addComponent(txtEmpKey, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlDetailEmpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(cmbEmpShop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlDetailEmpLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbEmpRank, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addGap(18, 18, 18)
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
                .addContainerGap(34, Short.MAX_VALUE))
        );
        pnlShopsLayout.setVerticalGroup(
            pnlShopsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlShopsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlShopsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlShopsLayout.createSequentialGroup()
                        .addGroup(pnlShopsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(pnlShopsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addGroup(pnlShopsLayout.createSequentialGroup()
                        .addGap(5, 5, 5)
                        .addComponent(pnlDetailsShop, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pnlDetailEmp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        tbtMain.addTab("Shops & Employees", pnlShops);

        tblProds.setAutoCreateRowSorter(true);
        tblProds.setModel(new javax.swing.table.DefaultTableModel(0, 2));
        tblProds.setAutoscrolls(false);
        tblProds.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane4.setViewportView(tblProds);
        tblProds.setTableHeader(null);
        javax.swing.table.TableColumnModel tcm = tblProds.getColumnModel();
        tcm.removeColumn( tcm.getColumn(0) );

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

        btnCatNew.setText("New");

        btnCatDelete.setText("Delete");

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
                .addContainerGap(31, Short.MAX_VALUE))
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

        btnProdNew.setText("New");

        btnProdDelete.setText("Delete");

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
                .addGap(0, 37, Short.MAX_VALUE)
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

        tblCheckProds.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, "test1",  new Integer(1)},
                {null, "test2",  new Integer(2)},
                {null, "test3",  new Integer(3)}
            },
            new String [] {
                "ID", "Product", "#"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.String.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblCheckProds.getTableHeader().setReorderingAllowed(false);
        tblSP.setViewportView(tblCheckProds);
        if (tblCheckProds.getColumnModel().getColumnCount() > 0) {
            tblCheckProds.getColumnModel().getColumn(0).setResizable(false);
            tblCheckProds.getColumnModel().getColumn(0).setPreferredWidth(50);
            tblCheckProds.getColumnModel().getColumn(1).setResizable(false);
            tblCheckProds.getColumnModel().getColumn(1).setPreferredWidth(250);
            tblCheckProds.getColumnModel().getColumn(2).setResizable(false);
            tblCheckProds.getColumnModel().getColumn(2).setPreferredWidth(50);
        }

        cmbCheckProd.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        btnCheckAdd.setText("Add");

        chkInvoice.setText("Invoice");
        chkInvoice.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkInvoiceActionPerformed(evt);
            }
        });

        btnCheckDel.setText("Remove");

        txtCheckNum.setText("1");

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
                            .addComponent(cmbInvCname, 0, 170, Short.MAX_VALUE)
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

        javax.swing.GroupLayout pnlCheckoutLayout = new javax.swing.GroupLayout(pnlCheckout);
        pnlCheckout.setLayout(pnlCheckoutLayout);
        pnlCheckoutLayout.setHorizontalGroup(
            pnlCheckoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlCheckoutLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlCheckoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(tblSP, javax.swing.GroupLayout.PREFERRED_SIZE, 294, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(pnlCheckoutLayout.createSequentialGroup()
                        .addComponent(txtCheckProdId)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbCheckProd, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(pnlCheckoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlCheckoutLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(pnlInvoice, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(pnlCheckoutLayout.createSequentialGroup()
                        .addGroup(pnlCheckoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlCheckoutLayout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addGroup(pnlCheckoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(pnlCheckoutLayout.createSequentialGroup()
                                        .addComponent(txtCheckNum, javax.swing.GroupLayout.PREFERRED_SIZE, 39, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(28, 28, 28)
                                        .addComponent(btnCheckAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(pnlCheckoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(chkInvoice)
                                        .addComponent(btnCheckDel))))
                            .addGroup(pnlCheckoutLayout.createSequentialGroup()
                                .addGap(68, 68, 68)
                                .addComponent(btnCheckDone, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        pnlCheckoutLayout.setVerticalGroup(
            pnlCheckoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlCheckoutLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlCheckoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlCheckoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(cmbCheckProd, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(pnlCheckoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtCheckNum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtCheckProdId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(btnCheckAdd))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlCheckoutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(tblSP, javax.swing.GroupLayout.PREFERRED_SIZE, 312, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(pnlCheckoutLayout.createSequentialGroup()
                        .addComponent(btnCheckDel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(chkInvoice)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(pnlInvoice, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCheckDone, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(5, 5, 5)))
                .addContainerGap(62, Short.MAX_VALUE))
        );

        tbtMain.addTab("Checkout", pnlCheckout);

        jLabel20.setText("From shop");

        cmbRepShop.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

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
                        .addComponent(bntRepGet, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(147, Short.MAX_VALUE))
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
                .addContainerGap(134, Short.MAX_VALUE))
        );

        tbtMain.addTab("Report", pnlReports);

        txtUser.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N

        txtPass.setFont(new java.awt.Font("Tahoma", 0, 16)); // NOI18N

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
                .addContainerGap(198, Short.MAX_VALUE))
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
                .addContainerGap(157, Short.MAX_VALUE))
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
        mnuFileGrp.add(mnuExit);

        jMenuBar1.add(mnuFileGrp);

        mnuUserGrp.setText("User");
        mnuUserGrp.setEnabled(false);

        mnuLogout.setText("Log out");
        mnuLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuLogoutActionPerformed(evt);
            }
        });
        mnuUserGrp.add(mnuLogout);

        mnuUinfo.setText("Info");
        mnuUserGrp.add(mnuUinfo);

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
        }
    }//GEN-LAST:event_btnLoginActionPerformed

    private void mnuLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuLogoutActionPerformed
        mnuUserGrp.setEnabled(false);
        mnuCommit.setEnabled(false);
        swapLayers(lpMain, pnlLogin, tbtMain);
        loggedIn = false;
        System.out.print("logged out");
    }//GEN-LAST:event_mnuLogoutActionPerformed

    private void mnuCommitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuCommitActionPerformed
        dbWrite.setForceCommit(true);
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

    
    private void tblProdsListSelection(ListSelectionEvent e){
        if (!e.getValueIsAdjusting()){
            int selected = tblProdsSM.getAnchorSelectionIndex();
            //System.out.println(selected);
            if (selected == 0 || selected == -1){ //-1 = nothing selected
                clearTxts(pnlDetailProd);
            } else {
                int absoluteProdIndex = tblProdsRS.convertRowIndexToModel(selected);
                //System.out.println(tblProdsM.getValueAt(tblProdsRS.convertRowIndexToModel(selected), 1));
                //int absoluteProdIndex = tblProds.convertRowIndexToModel(selected-1);
                //System.out.println(absoluteProdIndex);
                loadProduct(products.get(absoluteProdIndex - 1));
            }       
        }
    }
    
    /*when tabs are changed call the appropriate manager functions*/
    private void tbtMainStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tbtMainStateChanged
        if(loggedIn){
           //leaving panel
           switch(focusedTab){
               case PNL_SHOPS: pnlShopsExit(); break;
               case PNL_PRODUCTS: pnlProductsExit(); break;
               case PNL_CHECKOUT: pnlCheckoutExit(); break;
           }
           
           //entering panel
           focusedTab = tbtMain.getSelectedIndex();
           switch(focusedTab){
               case PNL_SHOPS: pnlShopsEnter(); break;
               case PNL_PRODUCTS: pnlProductsEnter(); break;
               case PNL_CHECKOUT: pnlCheckoutEnter(); break;
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
        //invoice checked
        if (chkInvoice.isSelected()){
            //clients
            int selected =  cmbInvCname.getSelectedIndex();
            Client tmpClient;
            Invoice inv;
            int newInvoiceKey;

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
            String timestamp = LocalDateTime.now().format(DBtimeFormat);
            
            inv = new Invoice(tmpClient.clientId, timestamp);
            
            newInvoiceKey = invoiceRepository.InsertGetKey(inv);
            inv.invoiceId = newInvoiceKey;
            
            //resest UI 
            chkInvoice.doClick();
            clearTxts(pnlInvoice);
        }
    }//GEN-LAST:event_btnCheckDoneActionPerformed

    private void lstCatsValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_lstCatsValueChanged
        //System.err.println(evt.toString());
        //category details
        if(!evt.getValueIsAdjusting()){ //list is updated
            int selected = lstCats.getSelectedIndex();
            if (selected == 0){ //new category
                clearTxts(pnlDetailsCat);
                tblProds.setEnabled(false);
                //remove all items from product table
                setRowFilterInt(tblProdsRS, new int [] {-2}, 0);
            } else {
                Category selectedCat = categories.get(selected - 1);
                loadCategory(selectedCat);
                tblProds.setEnabled(true);
                //-1 is the value for "new"
                setRowFilterInt(tblProdsRS, new int [] {selectedCat.productCategoriyId, -1}, 0);
            }
        }
    }//GEN-LAST:event_lstCatsValueChanged

    //<editor-fold defaultstate="collapsed" desc="autogenreated variables">
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bntRepGet;
    private javax.swing.JButton btnCatDelete;
    private javax.swing.JButton btnCatNew;
    private javax.swing.JButton btnCatUpdate;
    private javax.swing.JButton btnCheckAdd;
    private javax.swing.JButton btnCheckDel;
    private javax.swing.JButton btnCheckDone;
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
    private javax.swing.JComboBox<String> cmbCheckProd;
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
    private javax.swing.JLabel jLabel3;
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
    private javax.swing.JLabel lblPass;
    private javax.swing.JLabel lblPort;
    private javax.swing.JLabel lblUser;
    private javax.swing.JLayeredPane lpMain;
    private javax.swing.JList<String> lstCats;
    private javax.swing.JList<String> lstEmps;
    private javax.swing.JList<String> lstShops;
    private javax.swing.JMenuItem mnuCommit;
    private javax.swing.JMenuItem mnuExit;
    private javax.swing.JMenu mnuFileGrp;
    private javax.swing.JMenuItem mnuLogout;
    private javax.swing.JMenuItem mnuSaveLog;
    private javax.swing.JMenuItem mnuUinfo;
    private javax.swing.JMenu mnuUserGrp;
    private javax.swing.JPanel pnlCheckout;
    private javax.swing.JPanel pnlDetailEmp;
    private javax.swing.JPanel pnlDetailProd;
    private javax.swing.JPanel pnlDetailsCat;
    private javax.swing.JPanel pnlDetailsShop;
    private javax.swing.JPanel pnlInvoice;
    private javax.swing.JPanel pnlLogin;
    private javax.swing.JPanel pnlProducts;
    private javax.swing.JPanel pnlReports;
    private javax.swing.JPanel pnlShops;
    private javax.swing.JTable tblCheckProds;
    private javax.swing.JTable tblProds;
    private javax.swing.JScrollPane tblSP;
    private javax.swing.JTabbedPane tbtMain;
    private javax.swing.JTextArea txtCatDesc;
    private javax.swing.JTextField txtCatId;
    private javax.swing.JTextField txtCatName;
    private javax.swing.JTextField txtCheckNum;
    private javax.swing.JTextField txtCheckProdId;
    private javax.swing.JTextField txtEmpKey;
    private javax.swing.JTextField txtEmpName1;
    private javax.swing.JTextField txtEmpName2;
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
    private javax.swing.JTextField txtShopName;
    private javax.swing.JTextField txtUser;
    // End of variables declaration//GEN-END:variables
    //</editor-fold>
}
