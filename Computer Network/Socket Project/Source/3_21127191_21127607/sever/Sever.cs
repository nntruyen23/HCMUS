using System;
using System.Net;
using System.Net.Sockets;
using System.Text.RegularExpressions;
using System.Windows.Forms;
using System.Threading.Tasks;
using System.Runtime.Serialization.Formatters.Binary;
using System.Runtime.Serialization;
using System.Data;
using System.Data.SqlClient;
using System.Globalization;

namespace sever
{
    public partial class Sever : Form
    {
        public Sever()
        {
            InitializeComponent();
            CheckForIllegalCrossThreadCalls = false;
            Connect();
        }
        // dong ket noi
        private void Sever_FormClosed(object sender, FormClosedEventArgs e)
        {
            accountList.Clear();
            clientList.Clear();
            close();
        }
        // gui tin cho tat ca client
        private void btnSent_Click(object sender, EventArgs e)
        {
            foreach(Socket item in clientList)
            {
                Send(item);
            }
           
        }

        IPEndPoint IP;
        Socket sever;
        List<Socket> clientList;
        List<string> accountList;
       
        //connect
        void Connect()
        {
            //IP: dia chi cua sever
            clientList = new List<Socket>();
            accountList = new List<string>();
           
            IP = new IPEndPoint(IPAddress.Any, 8112);
            sever = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.IP);

            sever.Bind(IP);

            Thread Listen = new Thread(() => {
                try
                {
                    while (true)
                    {
                        //cho phep 100 client trong hang doi
                        sever.Listen(100);
                        Socket s = sever.Accept();
                        clientList.Add(s);
                        Thread receive = new Thread(Receive);
                        receive.IsBackground = true;
                        receive.Start(s);
                    }
                } 
                catch {
                    IP = new IPEndPoint(IPAddress.Any, 8112);
                    sever = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.IP);
                }                
            });
            Listen.IsBackground = true;
            Listen.Start();
        }
        void close()
        {
            sever.Close();
        }
        void Send(Socket s)
        {
            //if (s != null  && txbMessage.Text != String.Empty)
            //{
            //    s.Send(Serialize(txbMessage.Text));
            //}
        }
        void Receive(object obj)
        {
            Socket s = obj as Socket;
            try
            {
                while (true)
                {
                    byte[] data = new byte[1024 * 5000];

                    s.Receive(data);

                    string message = (string)Deserialize(data);
                    char command = message[0];
                    switch (command)
                    {
                        case '0':
                            {
                                string username0 = "";
                                int i0 = 2;
                                while (i0 < message.Length)
                                {
                                    username0 = username0 + message[i0];
                                    i0++;
                                }
                                clientList.Remove(s);
                                accountList.Remove(username0);
                            }
                            break;
                        //Resigter
                        case '1':
                            {
                                int c = checkResigter(message);
                                if (c == 0)
                                {
                                    //luu vao csdl
                                    s.Send(Serialize("10"));
                                }
                                else if (c == 1)
                                {
                                    s.Send(Serialize("11"));
                                }
                                else if (c == 2)
                                {
                                    s.Send(Serialize("12"));
                                }
                                else if (c == 3)
                                {
                                    s.Send(Serialize("13"));
                                }
                                else if (c == 4)
                                {
                                    s.Send(Serialize("14"));
                                }
                            }
                            
                            break;
                        //Login
                        case '2':
                            {
                                string username2 = "";
                                int i2 = 2;
                                while (message[i2] != ' ')
                                {
                                    username2 = username2 + message[i2];
                                    i2++;
                                }
                                if (!checkUserName(username2))
                                {
                                    s.Send(Serialize("22"));
                                }
                                else
                                {
                                    bool b = checkSignIn(message);
                                    if (b)
                                    {
                                        s.Send(Serialize("21 " + username2));
                                        accountList.Add(username2);
                                    }
                                    else
                                    {
                                        s.Send(Serialize("20"));
                                    }
                                }
                            }
                            break;
                        //Login success
                        case '3':
                            {
                                //string str = SelectDataPhong();
                            }
                            break;
                    }

                           
                    }
                }
            catch
            {
                clientList.Remove(s);
                s.Close();
            }
        }
        //add message vao khung chat
        //void AddMessage(string s)
        //{
        //    lsvMessage.Items.Add(new ListViewItem() { Text = s });
            
        //}
        //phan manh
        byte[] Serialize(object obj)
        {
            MemoryStream stream = new MemoryStream();
            BinaryFormatter formatter = new BinaryFormatter();

            formatter.Serialize(stream, obj);

            return stream.ToArray();

        }
        //gom manh
        object Deserialize(byte[] data)
        {
            MemoryStream stream = new MemoryStream(data);
            BinaryFormatter formatter = new BinaryFormatter();

            return formatter.Deserialize(stream);
        }
        //check tai khoan dang ki co hop le?
        int checkResigter(string data)
        {
            int i = 2;
            
            int count = 0;
            string username = "";
            string password = "";
            string bankAcc = "";
            //check user name
            while (data[i] != ' ' && (('a' <= data[i] && 'z' >= data[i]) || ('0' <= data[i] && '9' >= data[i])))
            {
                username = username + data[i];
                i++;
                count++;
            }
            if (count < 5) return 1;

            count = 0;
            i++;
            // check password
            while (data[i] != ' ')
            {
                password = password + data[i];
                i++;
                count++;
            }
            if (count < 3) return 2;
            count = 0;
            i++;

            //check bank account
            while (i<data.Length && ('0' <= data[i] && '9' >= data[i]))
            {
                bankAcc = bankAcc + data[i];
                i++;
                count++;
            }
            if (count != 10) return 3;
            SqlConnection conn = new SqlConnection(@"Data Source=LAPTOP-DF1LB0CM\NHATTRUYEN;Initial Catalog=Account;Integrated Security=True");
            try
            {
                conn.Open();
                string sql = "select *from account where UserName='" + username +  "'";
                SqlCommand cmd = new SqlCommand(sql, conn);
                SqlDataReader dta = cmd.ExecuteReader();
                if (dta.Read() == true) { return 4; }
                conn.Close();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Connection Eror");
            }

            try
            {
                conn.Open();
                string sql = "Insert into account values ('" + username + "','" + password + "','" + bankAcc + "')";
                SqlCommand cmd = new SqlCommand(sql, conn);
                cmd.ExecuteNonQuery();
                conn.Close();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Connection Eror");
            }
            return 0;
        }
       
        bool checkSignIn(string data)
        {
            string username = "";
            string password = "";
            int i = 2;
            while(data[i] != ' ')
            {
                username = username + data[i];
                i++;
            }
            i++;
            while (i < data.Length) 
            {
                password = password + data[i];
                i++;
            }
            
            SqlConnection conn = new SqlConnection(@"Data Source=LAPTOP-DF1LB0CM\NHATTRUYEN;Initial Catalog=Account;Integrated Security=True");
            try
            {
                conn.Open();
                string sql = "select *from account where UserName='" + username + "' and PassWord='" + password + "'";
                SqlCommand cmd = new SqlCommand(sql, conn);
                SqlDataReader dta = cmd.ExecuteReader();
                if (dta.Read() == true)
                {
                    conn.Close();
                    return true;
                }
                else
                {
                    conn.Close();
                    return false;
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show("Connection Eror");
                return false;
            }
        }
        bool checkUserName(string username)
        {
            bool check = true;
            accountList.ForEach(delegate (string name) {
                if (check && name == username) check = false;
            });
            return check;
        }

        private void Sever_Load(object sender, EventArgs e)
        {
            loadData();
        }
        public void InsertPhong(string fileName,byte[] image,string name, string type, float cost, int amount, string detail)
        {
            SqlConnection conn = new SqlConnection(@"Data Source=LAPTOP-DF1LB0CM\NHATTRUYEN;Initial Catalog=Account;Integrated Security=True");
            try
            {
                if(conn.State == ConnectionState.Closed)
                {
                    conn.Open();
                }
                using (SqlCommand cmd = new SqlCommand("insert into phong(FileName, Image, Ten, LoaiPhong, MoTa, Gia, SoLuong) values(@FileName, @Image, @Ten, @LoaiPhong, @MoTa, @Gia, @SoLuong)", conn))
                {
                    cmd.CommandType = CommandType.Text;
                    cmd.Parameters.AddWithValue("@FileName", fileName);
                    cmd.Parameters.AddWithValue("@Image", image);
                    cmd.Parameters.AddWithValue("@Ten", name);
                    cmd.Parameters.AddWithValue("@LoaiPhong", type);
                    cmd.Parameters.AddWithValue("@MoTa", detail);
                    cmd.Parameters.AddWithValue("@Gia", cost);
                    cmd.Parameters.AddWithValue("@SoLuong", amount);
                    cmd.ExecuteNonQuery();
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show("Connection Eror");
            }
            conn.Close();
        }
        public void loadData()
        {

            SqlConnection conn = new SqlConnection(@"Data Source=LAPTOP-DF1LB0CM\NHATTRUYEN;Initial Catalog=Account;Integrated Security=True");
            try
            {
                if (conn.State == ConnectionState.Closed)
                {
                    conn.Open();
                }
                using (DataTable dt = new DataTable("phong"))
                {
                    SqlDataAdapter adapter = new SqlDataAdapter("select *from phong", conn);
                    adapter.Fill(dt);
                    dataGridView.DataSource = dt;
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show("Connection Eror");
            }

            conn.Close();
        }
        byte[] ConvertImageToBytes(Image img)
        {
            using(MemoryStream ms = new MemoryStream())
            {
                img.Save(ms,System.Drawing.Imaging.ImageFormat.Png);
                return ms.ToArray();
            }
        }
        public Image ConvertByteArrayToImage(byte[] data)
        {
            using (MemoryStream ms = new MemoryStream(data))
            {
                return Image.FromStream(ms);
            }
        }
        private void button1_Click(object sender, EventArgs e)
        {
            using(OpenFileDialog ofd = new OpenFileDialog() {Filter = "Image files(*.jpg;*jepg;*png)|*.jpg;*jepg;*png", Multiselect=false })
            {
                if(ofd.ShowDialog() == DialogResult.OK)
                {
                    //display image to picturepox
                    pictureBox.Image = Image.FromFile(ofd.FileName);
                    //set path
                    txtFileName.Text = ofd.FileName;
                    //insert data to local database, then reload data
                    float cost = float.Parse(txtCost.Text, CultureInfo.InvariantCulture.NumberFormat);
                    int Amount = Int32.Parse(txtAmount.Text);
                    
                    InsertPhong(txtFileName.Text, ConvertImageToBytes(pictureBox.Image), txtName.Text, txtType.Text, cost, Amount, txtDetail.Text);
                    loadData();
                }
            }
        }

        private void dataGridView_CellClick(object sender, DataGridViewCellEventArgs e)
        {
            DataTable dt = dataGridView.DataSource as DataTable;
            if(dt != null)
            {
                DataRow row = dt.Rows[e.RowIndex];
                pictureBox.Image = ConvertByteArrayToImage((byte[])row["Image"]);
                txtName.Text = dataGridView.Rows[e.RowIndex].Cells[1].Value.ToString();
                txtType.Text = dataGridView.Rows[e.RowIndex].Cells[4].Value.ToString();
                txtCost.Text = dataGridView.Rows[e.RowIndex].Cells[6].Value.ToString();
                txtAmount.Text = dataGridView.Rows[e.RowIndex].Cells[7].Value.ToString();
                txtDetail.Text = dataGridView.Rows[e.RowIndex].Cells[5].Value.ToString();
            }
        }

        private void label7_Click(object sender, EventArgs e)
        {
            txtName.Clear();
            txtType.Clear();
            txtCost.Clear();
            txtAmount.Clear();
            txtDetail.Clear();
            pictureBox.Image = null; ;
        }
        

    }
}