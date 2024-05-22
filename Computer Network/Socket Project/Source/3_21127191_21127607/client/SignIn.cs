using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Net;
using System.Net.Sockets;
using System.Runtime.Serialization.Formatters.Binary;
namespace client
{ 
    public partial class SignIn : Form
    {
        //Socket client;
        public SignIn()
        {
            InitializeComponent();
            CheckForIllegalCrossThreadCalls = false;
            Connect();
            txtUserName.Focus();
           
        }

        private void button1_Click(object sender, EventArgs e)
        {

            if (txtUserName.Text != null && txtPassWord.Text != null)
            {
                Send("2 " + txtUserName.Text + " " + txtPassWord.Text);
            }
            Receive();
        }

        private void label2_Click(object sender, EventArgs e)
        {
            txtUserName.Clear();
            txtPassWord.Clear();
            txtUserName.Focus();
        }

        private void label3_Click(object sender, EventArgs e)
        {
            this.Hide();
            client.Close();
        }

        private void button2_Click(object sender, EventArgs e)
        {
            if (txtPassWord.UseSystemPasswordChar == false)
            {
                button3.BringToFront();
                txtPassWord.UseSystemPasswordChar = true;
            }
        }

        private void button3_Click(object sender, EventArgs e)
        {
            if (txtPassWord.UseSystemPasswordChar == true)
            {
                button2.BringToFront();
                txtPassWord.UseSystemPasswordChar = false;
            }
        }

        private void SignIn_Load(object sender, EventArgs e)
        {

        }

        private void label4_Click(object sender, EventArgs e)
        {
            new Resigter(client).Show();
            this.Hide();
        }

        private void textBox1_TextChanged(object sender, EventArgs e)
        {

        }

        private void txtPassWord_TextChanged(object sender, EventArgs e)
        {

        }

        private void SignIn_FormClosed(object sender, FormClosedEventArgs e)
        {
            Close();
        }
        IPEndPoint IP;
        Socket client;
        void Connect()
        {
            //IP: dia chi cua sever
            IP = new IPEndPoint(IPAddress.Parse("127.0.0.1"), 8112);
            client = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.IP);
            try
            {
                client.Connect(IP);
            }
            catch
            {
                MessageBox.Show("Không thể kết nối với sever!", "Eror", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return;
            }
       
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
                

                    byte[] data = new byte[1024 * 5000];

                    client.Receive(data);

                    string message = (string)Deserialize(data);
                    string command = "";
                    command = command + message[0] + message[1];
                    if (command == null)
                    {
                        MessageBox.Show("SignIn unsuccess! Again.");
                    }
                    else if (command == "20")
                    {
                        MessageBox.Show("SignIn unsuccess! Again.");
                    }
                    else if (command == "21")
                    {
                        string username = "";
                        int i = 3;
                        while (i < message.Length)
                        {
                            username = username + message[i];
                            i++;
                        }
                        MessageBox.Show("SignIn success.");
                        new Client(client, username).Show();
                        this.Hide();
                        //return;
                    }
                    else if (command == "22")
                    {
                        MessageBox.Show("SignIn unsuccess! Again.");
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

    }
}
