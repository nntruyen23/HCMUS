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
namespace client
{
    public partial class Client : Form
    {
        Socket client;
        string UserName;
        public Client(Socket s, string username)
        {

            InitializeComponent();
            UserName = username;
           
            Connect();
            loadData();
        }
        
        private void FormClient_Load(object sender, EventArgs e)
        {
            label1.Text = UserName;

        }
        //dong ket noi khi dong form
        private void Client_FormClosed(object sender, FormClosedEventArgs e)
        {
            Send("0 " + UserName);
            Close();
        }
        //connect
        void Connect()
        {
            Thread listen = new Thread(Receive);
            listen.IsBackground = true;
            listen.Start();
        }
        void close()
        {
            client.Close();
        }
        public void Send(String data)
        {


            if (data != String.Empty)
            {
                client.Send(Serialize(data));
            }
        }
       
        public void Receive()
        {
            try
            {
                while (true)
                {
                    
                    byte[] data = new byte[1024 * 5000];
                    client.Receive(data);
                    string message = (string)Deserialize(data);
                    
                    
                }
            }
            catch
            {
                close();
            }
        }

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

        private void button1_Click(object sender, EventArgs e)
        {
            Send("0 " + UserName);
            new SignIn().Show();
            this.Hide();
        }

        private void label1_Click(object sender, EventArgs e)
        {

        }

       
        byte[] ConvertImageToBytes(Image img)
        {
            using (MemoryStream ms = new MemoryStream())
            {
                img.Save(ms, System.Drawing.Imaging.ImageFormat.Png);
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
        void loadData()
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
                    if (dt != null)
                    {
                        foreach(DataRow row in dt.Rows)
                        {
                            flowLayoutPanel1.Controls.Add(new PictureBox {
                                //name
                                Size = new Size(250, 250),
                                SizeMode = PictureBoxSizeMode.StretchImage,
                                BorderStyle = BorderStyle.Fixed3D,
                                BackColor = Color.White,
                                Image = ConvertByteArrayToImage((byte[])row["Image"]),
                            });

                            ListView lv = new ListView() {
                                //name
                                Size = new Size(1100, 250),
                                BackColor = Color.White,
                                View = View.List,

                            };
                            lv.Items.Add(new ListViewItem() {
                                Font = new Font("Arial", 14, System.Drawing.FontStyle.Regular),
                                ForeColor = Color.FromArgb(0, 117, 214),
                                Text = " ",
                            });
                            lv.Items.Add(new ListViewItem() {
                                Font = new Font("Arial", 14, System.Drawing.FontStyle.Regular),
                                ForeColor = Color.FromArgb(0, 117, 214),
                                Text = " ",
                            });
                            lv.Items.Add(new ListViewItem() {
                                Font = new Font("Arial", 14, System.Drawing.FontStyle.Regular),
                                ForeColor = Color.FromArgb(0, 117, 214),
                                Text = "  Tên: " + row["Ten"].ToString(),
                            });
                            lv.Items.Add(new ListViewItem() {
                                Font = new Font("Arial", 14, System.Drawing.FontStyle.Regular),
                                ForeColor = Color.FromArgb(0, 117, 214),
                                Text = " ",
                            });

                            lv.Items.Add(new ListViewItem() {
                                Font = new Font("Arial", 14, System.Drawing.FontStyle.Regular),
                                ForeColor = Color.FromArgb(0, 117, 214),
                                Text = "  Loại phòng: " + row["LoaiPhong"].ToString(),
                            });
                            lv.Items.Add(new ListViewItem() {
                                Font = new Font("Arial", 14, System.Drawing.FontStyle.Regular),
                                ForeColor = Color.FromArgb(0, 117, 214),
                                Text = " ",
                            });

                            lv.Items.Add(new ListViewItem() {
                                Font = new Font("Arial", 14, System.Drawing.FontStyle.Regular),
                                ForeColor = Color.FromArgb(0, 117, 214),
                                Text = "  Giá: " + row["Gia"].ToString(),
                            });
                            lv.Items.Add(new ListViewItem() {
                                Font = new Font("Arial", 14, System.Drawing.FontStyle.Regular),
                                ForeColor = Color.FromArgb(0, 117, 214),
                                Text = " ",
                            });

                            lv.Items.Add(new ListViewItem() {

                                Font = new Font("Arial", 8, System.Drawing.FontStyle.Regular),
                                ForeColor = Color.FromArgb(0, 117, 214),
                                Text = "    Mô Tả: " + row["MoTa"].ToString(),
                            });

                            flowLayoutPanel1.Controls.Add(lv);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show("Connection Eror");
            }

            conn.Close();
        }
        private void flowLayoutPanel1_Paint(object sender, PaintEventArgs e)
        {

        }

        private void button2_Click(object sender, EventArgs e)
        {
             flowLayoutPanel1.Controls.Clear();
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
                    if (dt != null)
                    {
                        foreach (DataRow row in dt.Rows)
                        if(row["Ten"].ToString() == txtSearch.Text)
                        {
                            flowLayoutPanel1.Controls.Add(new PictureBox {
                                //name
                                Size = new Size(250, 250),
                                SizeMode = PictureBoxSizeMode.StretchImage,
                                BorderStyle = BorderStyle.Fixed3D,
                                BackColor = Color.White,
                                Image = ConvertByteArrayToImage((byte[])row["Image"]),
                            });

                            ListView lv = new ListView() {
                                //name
                                Size = new Size(1100, 250),
                                BackColor = Color.White,
                                View = View.List,

                            };
                            lv.Items.Add(new ListViewItem() {
                                Font = new Font("Arial", 14, System.Drawing.FontStyle.Regular),
                                ForeColor = Color.FromArgb(0, 117, 214),
                                Text = " ",
                            });
                            lv.Items.Add(new ListViewItem() {
                                Font = new Font("Arial", 14, System.Drawing.FontStyle.Regular),
                                ForeColor = Color.FromArgb(0, 117, 214),
                                Text = " ",
                            });
                            lv.Items.Add(new ListViewItem() {
                                Font = new Font("Arial", 14, System.Drawing.FontStyle.Regular),
                                ForeColor = Color.FromArgb(0, 117, 214),
                                Text = "  Tên: " + row["Ten"].ToString(),
                            });
                            lv.Items.Add(new ListViewItem() {
                                Font = new Font("Arial", 14, System.Drawing.FontStyle.Regular),
                                ForeColor = Color.FromArgb(0, 117, 214),
                                Text = " ",
                            });

                            lv.Items.Add(new ListViewItem() {
                                Font = new Font("Arial", 14, System.Drawing.FontStyle.Regular),
                                ForeColor = Color.FromArgb(0, 117, 214),
                                Text = "  Loại phòng: " + row["LoaiPhong"].ToString(),
                            });
                            lv.Items.Add(new ListViewItem() {
                                Font = new Font("Arial", 14, System.Drawing.FontStyle.Regular),
                                ForeColor = Color.FromArgb(0, 117, 214),
                                Text = " ",
                            });

                            lv.Items.Add(new ListViewItem() {
                                Font = new Font("Arial", 14, System.Drawing.FontStyle.Regular),
                                ForeColor = Color.FromArgb(0, 117, 214),
                                Text = "  Giá: " + row["Gia"].ToString(),
                            });
                            lv.Items.Add(new ListViewItem() {
                                Font = new Font("Arial", 14, System.Drawing.FontStyle.Regular),
                                ForeColor = Color.FromArgb(0, 117, 214),
                                Text = " ",
                            });

                            lv.Items.Add(new ListViewItem() {

                                Font = new Font("Arial", 8, System.Drawing.FontStyle.Regular),
                                ForeColor = Color.FromArgb(0, 117, 214),
                                Text = "    Mô Tả: " + row["MoTa"].ToString(),
                            });

                            flowLayoutPanel1.Controls.Add(lv);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show("Connection Eror");
            }

            conn.Close();
        }

        private void pictureBox1_Click(object sender, EventArgs e)
        {
            flowLayoutPanel1.Controls.Clear();
            loadData();
            txtSearch.Clear();
        }

        private void button2_Click_1(object sender, EventArgs e)
        {
            new Booking(UserName).Show();
        }
    }
}