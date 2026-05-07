const express = require("express");
const bodyParser = require("body-parser");
const cors = require("cors");
const jwt = require("jsonwebtoken");
const fs = require("fs");
const path = require("path");

const app = express();
const SECRET_KEY = "phim_bi_mat_rat_bao_mat_123";
const DATA_FILE = path.join(__dirname, "data.json");

// Tăng giới hạn payload để nhận ảnh Base64 từ mobile app
app.use(cors());
app.use(bodyParser.json({ limit: '50mb' }));
app.use(bodyParser.urlencoded({ limit: '50mb', extended: true }));

const readData = () => {
  try {
    if (!fs.existsSync(DATA_FILE)) {
      return { users: [], transactions: [], categories: [] };
    }
    const data = JSON.parse(fs.readFileSync(DATA_FILE));
    if (!data.users) data.users = [];
    if (!data.transactions) data.transactions = [];
    if (!data.categories) data.categories = [];
    return data;
  } catch (e) {
    return { users: [], transactions: [], categories: [] };
  }
};

const saveData = (data) => {
  fs.writeFileSync(DATA_FILE, JSON.stringify(data, null, 2));
};

const defaultCategories = [
  { name: "Ăn uống", type: "EXPENSE" },
  { name: "Di chuyển", type: "EXPENSE" },
  { name: "Mua sắm", type: "EXPENSE" },
  { name: "Giải trí", type: "EXPENSE" },
  { name: "Sức khỏe", type: "EXPENSE" },
  { name: "Lương", type: "INCOME" },
  { name: "Thưởng", type: "INCOME" },
  { name: "Đầu tư", type: "INCOME" },
  { name: "Khác", type: "INCOME" },
];

const initializeDefaultCategories = (data, userId) => {
  const userCategories = data.categories.filter(c => c.userId === userId);
  if (userCategories.length === 0) {
    defaultCategories.forEach(cat => {
      data.categories.push({
        id: Date.now().toString() + Math.random().toString(36).substr(2, 5),
        name: cat.name,
        type: cat.type,
        userId: userId
      });
    });
    return true;
  }
  return false;
};

const authenticateToken = (req, res, next) => {
  const authHeader = req.headers["authorization"];
  const token = authHeader && authHeader.split(" ")[1];
  if (!token) return res.status(401).json({ message: "Vui lòng đăng nhập" });

  jwt.verify(token, SECRET_KEY, (err, user) => {
    if (err) return res.status(403).json({ message: "Phiên đăng nhập hết hạn" });
    req.user = user;
    next();
  });
};

// --- AUTH API ---
app.post("/api/auth/register", (req, res) => {
  const { username, email, password } = req.body;
  const data = readData();
  if (data.users.find(u => u.email === email)) return res.status(400).json({ message: "Email đã tồn tại" });

  const userId = Date.now().toString();
  const newUser = { id: userId, username, email, password, avatar: "" };
  data.users.push(newUser);

  // Khởi tạo danh mục mặc định ngay khi đăng ký
  initializeDefaultCategories(data, userId);

  saveData(data);
  res.status(200).json({ message: "Đăng ký thành công" });
});

app.post("/api/auth/login", (req, res) => {
  const { email, password } = req.body;
  const data = readData();
  const user = data.users.find(u => u.email === email && u.password === password);

  if (user) {
    // Kiểm tra và bổ sung danh mục mặc định nếu user cũ chưa có
    if (initializeDefaultCategories(data, user.id)) {
      saveData(data);
    }
    const token = jwt.sign({ id: user.id, email: user.email }, SECRET_KEY, { expiresIn: '24h' });
    res.status(200).json({ token, message: "Đăng nhập thành công" });
  } else {
    res.status(401).json({ message: "Sai email hoặc mật khẩu" });
  }
});

app.get("/api/auth/profile", authenticateToken, (req, res) => {
  const data = readData();
  const user = data.users.find(u => u.id === req.user.id);
  if (user) {
    const { password, ...userWithoutPassword } = user;
    res.json(userWithoutPassword);
  } else {
    res.status(404).json({ message: "User không tồn tại" });
  }
});

app.put("/api/auth/profile", authenticateToken, (req, res) => {
  const data = readData();
  const index = data.users.findIndex(u => u.id === req.user.id);
  if (index !== -1) {
    data.users[index].username = req.body.username || data.users[index].username;
    data.users[index].avatar = req.body.avatar || data.users[index].avatar;
    saveData(data);
    const { password, ...userWithoutPassword } = data.users[index];
    res.json(userWithoutPassword);
  } else {
    res.status(404).json({ message: "User không tồn tại" });
  }
});

// --- CATEGORIES API ---
app.get("/api/categories", authenticateToken, (req, res) => {
  const data = readData();
  let userCategories = data.categories.filter(c => c.userId === req.user.id);

  // Fallback: Nếu vì lý do nào đó vẫn trống, khởi tạo lại
  if (userCategories.length === 0) {
      initializeDefaultCategories(data, req.user.id);
      saveData(data);
      userCategories = data.categories.filter(c => c.userId === req.user.id);
  }

  res.json(userCategories);
});

app.post("/api/categories", authenticateToken, (req, res) => {
  const data = readData();
  const newCategory = { ...req.body, id: Date.now().toString(), userId: req.user.id };
  data.categories.push(newCategory);
  saveData(data);
  res.status(201).json(newCategory);
});

app.put("/api/categories/:id", authenticateToken, (req, res) => {
  const data = readData();
  const index = data.categories.findIndex(c => c.id === req.params.id && c.userId === req.user.id);
  if (index !== -1) {
    data.categories[index] = { ...data.categories[index], ...req.body, id: req.params.id, userId: req.user.id };
    saveData(data);
    res.json(data.categories[index]);
  } else {
    res.status(404).json({ message: "Không tìm thấy danh mục" });
  }
});

app.delete("/api/categories/:id", authenticateToken, (req, res) => {
  let data = readData();
  data.categories = data.categories.filter(c => !(c.id === req.params.id && c.userId === req.user.id));
  saveData(data);
  res.json({ message: "Xóa thành công" });
});

// --- TRANSACTIONS API ---
app.get("/api/transactions", authenticateToken, (req, res) => {
  const data = readData();
  res.json(data.transactions.filter(t => t.userId === req.user.id));
});

app.post("/api/transactions", authenticateToken, (req, res) => {
  const data = readData();
  const newTransaction = { ...req.body, id: Date.now().toString(), userId: req.user.id };
  data.transactions.push(newTransaction);
  saveData(data);
  res.status(201).json(newTransaction);
});

app.put("/api/transactions/:id", authenticateToken, (req, res) => {
  const data = readData();
  const index = data.transactions.findIndex(t => t.id === req.params.id && t.userId === req.user.id);
  if (index !== -1) {
    data.transactions[index] = { ...data.transactions[index], ...req.body, id: req.params.id, userId: req.user.id };
    saveData(data);
    res.json(data.transactions[index]);
  } else {
    res.status(404).json({ message: "Không tìm thấy giao dịch" });
  }
});

app.delete("/api/transactions/:id", authenticateToken, (req, res) => {
  let data = readData();
  data.transactions = data.transactions.filter(t => !(t.id === req.params.id && t.userId === req.user.id));
  saveData(data);
  res.json({ message: "Xóa thành công" });
});

// Sửa đoạn này để chạy được trên Cloud
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});
