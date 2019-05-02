import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import oracle.jdbc.proxy.annotation.Pre;
import org.apache.commons.lang3.SerializationUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.xml.transform.Result;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.ArrayList;

public class MainOffice {

    public java.sql.Connection connection;
    public String url="main";
    public ArrayList<MainSales> sales;
    public static final String QUEUE_1 = "queue_1";
    public static  final String QUEUE_2 = "queue_2";
    private static String[] columnNames={"date","region","product","qty","cost","total"};

    public MainOffice(){
        sales=new ArrayList<MainSales>();
    }

    public void addSale(MainSales sale){
        this.sales.add(sale);
    }

    public void connectToDb(){
        try {
            Class.forName ("org.h2.Driver");
            this.connection = DriverManager.
                    getConnection("jdbc:h2:~/"+url, "sa", "");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void init(){
        connectToDb();
        JFrame frame=new JFrame("Main Office");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JButton jButton1=new JButton("show data");
        jButton1.setBounds(120,130,150,20);
        jButton1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fetch_data();
                show_data();
            }
        });
        frame.add(jButton1);
        frame.setLayout(new FlowLayout());
        frame.setVisible(true);
        frame.setSize(500, 400);
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try{
            Connection connection_1 =factory.newConnection();
            Channel channel_1 = connection_1.createChannel();
            channel_1.queueDeclare(QUEUE_1, false, false, false, null);
            DeliverCallback deliverCallback_1 = (consumerTag, delivery) -> {
                ArrayList<Sales> sales= SerializationUtils.deserialize( delivery.getBody());
                System.out.println(sales.size());
                insert_data(sales);
            };
            channel_1.basicConsume(QUEUE_1, true, deliverCallback_1, (consumerTag) -> {
            });


            Connection connection_2 =factory.newConnection();
            Channel channel_2 = connection_2.createChannel();
            channel_2.queueDeclare(QUEUE_2, false, false, false, null);
            DeliverCallback deliverCallback_2 = (consumerTag, delivery) -> {
                ArrayList<Sales> sales= SerializationUtils.deserialize( delivery.getBody());
                System.out.println(sales.size());
                insert_data(sales);

                };
            channel_1.basicConsume(QUEUE_2, true, deliverCallback_2, (consumerTag) -> {
            });

        }catch(Exception e){

        }
    }

    public void fetch_data(){
        try {
            String req = "select * from sales";
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(req);
            this.sales.clear();
            while (resultSet.next()) {
                int id=resultSet.getInt(1);
                Date date =resultSet.getDate(2);
                String region=resultSet.getString(3);
                String product=resultSet.getString(4);
                int qty=resultSet.getInt(5);
                double cost=resultSet.getDouble(6);
                double total=resultSet.getDouble(7);
                //int boId=resultSet.getInt(7);
                MainSales sale=new MainSales(id,date,region,product,qty,cost,total,5);
                this.addSale(sale);
            }

        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public void show_data(){
        JFrame frame = new JFrame("Database Search Result");
        //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
//TableModel tm = new TableModel();
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(columnNames);
        JTable table = new JTable();
        table.setModel(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        //table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        String date= "";
        String region= "";
        String product= "";
        String qty= "";
        String cost="";
        String total="";
        for (int i=0; i<sales.size(); i++){
            date=sales.get(i).getDate().toString();
            region=sales.get(i).getRegion();
            product=sales.get(i).getProduct();
            Integer qt=new Integer(sales.get(i).getQty());
            qty = qt.toString();
            Double cst=new Double(sales.get(i).getCost());
            cost=cst.toString();
            Double tot=new Double(sales.get(i).getTotal());
            total=tot.toString();
            model.addRow(new Object[]{date,region,product, qty,cost,total});
        }
        frame.add(scroll);
        frame.setVisible(true);
        frame.setSize(400,300);
    }

    public void insert_data(ArrayList<Sales> sales){
        try {
            PreparedStatement statement1=connection.prepareStatement("Select * from sales where boid=? and region=?");
            PreparedStatement statement2=connection.prepareStatement("Insert into sales(date,region,product,qty,cost,total,boid) " +
                    "values (?,?,?,?,?,?,?)");
            for(int i=0;i<sales.size();i++){
                statement1.setInt(1,sales.get(i).getId());
                statement1.setString(2,sales.get(i).getRegion());
                ResultSet res=statement1.executeQuery();
                if(! res.next()){
                    statement2.setDate(1,sales.get(i).getDate());
                    statement2.setString(2,sales.get(i).getRegion());
                    statement2.setString(3,sales.get(i).getProduct());
                    statement2.setInt(4,sales.get(i).getQty());
                    statement2.setDouble(5,sales.get(i).getCost());
                    statement2.setDouble(6,sales.get(i).getTotal());
                    statement2.setInt(7,sales.get(i).getId());
                    statement2.executeUpdate();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main (String [] args){
        MainOffice mainOffice=new MainOffice();
        mainOffice.init();

    }

}
