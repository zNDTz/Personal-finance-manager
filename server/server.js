require("dotenv").config();
const express = require("express");
const bodyParser = require("body-parser");
const cors = require("cors");
const jwt = require("jsonwebtoken");
const mongoose = require("mongoose");

const app = express();

// Kết nối MongoDB
mongoose
  .connect(process.env.MONGO_URI)
  .then(() => console.log("Connected to MongoDB successfully"))
  .catch((err) => console.error("MongoDB connection error:", err));

const transformRes = (doc, ret) => {
  ret.id = ret._id.toString();
  delete ret._id;
  delete ret.__v;
};

// --- Models ---
const UserSchema = new mongoose.Schema({
  username: { type: String, required: true },
  email: { type: String, required: true, unique: true },
  password: { type: String, required: true },
  avatar: { type: String, default: "" },
});
UserSchema.set("toJSON", { transform: transformRes });
const User = mongoose.model("User", UserSchema);

const CategorySchema = new mongoose.Schema({
  name: { type: String, required: true },
  type: { type: String, enum: ["INCOME", "EXPENSE"], required: true },
  userId: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: true },
  budget: { type: Number, default: 0 },
});
CategorySchema.set("toJSON", { transform: transformRes });
const Category = mongoose.model("Category", CategorySchema);

const WalletSchema = new mongoose.Schema({
  name: { type: String, required: true },
  balance: { type: Number, default: 0 },
  userId: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: true },
});
WalletSchema.set("toJSON", { transform: transformRes });
const Wallet = mongoose.model("Wallet", WalletSchema);

const TransactionSchema = new mongoose.Schema({
  userId: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: true },
  walletId: { type: mongoose.Schema.Types.ObjectId, ref: "Wallet", required: true },
  categoryId: { type: String },
  amount: { type: Number, required: true },
  type: { type: String, enum: ["INCOME", "EXPENSE"], required: true },
  categoryName: { type: String, required: true },
  description: { type: String, default: "" },
  date: { type: String, required: true },
});
TransactionSchema.set("toJSON", { transform: transformRes });
const Transaction = mongoose.model("Transaction", TransactionSchema);

// Middleware
app.use(cors());
app.use(bodyParser.json({ limit: "50mb" }));
app.use(bodyParser.urlencoded({ limit: "50mb", extended: true }));

// Log yêu cầu để debug
app.use((req, res, next) => {
  console.log(`[${new Date().toLocaleTimeString()}] ${req.method} ${req.url}`);
  next();
});

const authenticateToken = (req, res, next) => {
  const authHeader = req.headers["authorization"];
  const token = authHeader && authHeader.split(" ")[1];
  if (!token) {
    console.log("Auth failed: No token provided");
    return res.status(401).json({ message: "Vui lòng đăng nhập" });
  }

  jwt.verify(token, process.env.SECRET_KEY || "YOUR_SECRET_KEY", (err, user) => {
    if (err) {
      console.log("Auth failed: Invalid or expired token");
      return res.status(403).json({ message: "Hết hạn đăng nhập" });
    }
    req.user = user;
    next();
  });
};

const initializeDefaults = async (userId) => {
  const catCount = await Category.countDocuments({ userId });
  if (catCount === 0) {
    const defaults = [
      { name: "Ăn uống", type: "EXPENSE" },
      { name: "Lương", type: "INCOME" },
      { name: "Mua sắm", type: "EXPENSE" },
      { name: "Giải trí", type: "EXPENSE" },
    ];
    await Category.insertMany(defaults.map((c) => ({ ...c, userId })));
  }
  const walletCount = await Wallet.countDocuments({ userId });
  if (walletCount === 0) {
    await new Wallet({ name: "Ví chính", balance: 0, userId }).save();
  }
};

// --- API AUTH ---
app.post("/api/auth/register", async (req, res) => {
  const { username, email, password } = req.body;
  try {
    const existingUser = await User.findOne({ email });
    if (existingUser) return res.status(400).json({ message: "Email đã tồn tại" });
    const newUser = new User({ username, email, password });
    await newUser.save();
    await initializeDefaults(newUser._id);
    res.status(200).json({ message: "Đăng ký thành công" });
  } catch (error) {
    res.status(500).json({ message: "Lỗi server khi đăng ký" });
  }
});

app.post("/api/auth/login", async (req, res) => {
  const { email, password } = req.body;
  try {
    const user = await User.findOne({ email, password });
    if (user) {
      await initializeDefaults(user._id);
      const token = jwt.sign(
        { id: user._id, email: user.email },
        process.env.SECRET_KEY || "YOUR_SECRET_KEY",
        { expiresIn: "24h" }
      );
      res.status(200).json({ token, message: "Đăng nhập thành công" });
    } else res.status(401).json({ message: "Sai email hoặc mật khẩu" });
  } catch (error) {
    res.status(500).json({ message: "Lỗi server khi đăng nhập" });
  }
});

app.get("/api/auth/profile", authenticateToken, async (req, res) => {
  try {
    const user = await User.findById(req.user.id).select("-password");
    if (!user) return res.status(404).json({ message: "Người dùng không tồn tại" });
    res.json(user);
  } catch (error) {
    res.status(500).json({ message: "Lỗi server" });
  }
});

app.put("/api/auth/profile", authenticateToken, async (req, res) => {
  try {
    const { username, email, avatar } = req.body;
    if (email) {
      const existing = await User.findOne({ email, _id: { $ne: req.user.id } });
      if (existing) return res.status(400).json({ message: "Email đã được sử dụng bởi người khác" });
    }
    const updateData = {};
    if (username) updateData.username = username;
    if (email) updateData.email = email;
    if (avatar) updateData.avatar = avatar;
    const user = await User.findByIdAndUpdate(req.user.id, { $set: updateData }, { new: true }).select("-password");
    res.json(user);
  } catch (error) {
    res.status(500).json({ message: "Lỗi server" });
  }
});

// API Đổi mật khẩu
app.post("/api/auth/change-password", authenticateToken, async (req, res) => {
  console.log("Request change password for user:", req.user.id);
  try {
    const { currentPassword, oldPassword, newPassword } = req.body;
    const pwdInput = currentPassword || oldPassword;

    const user = await User.findById(req.user.id);
    if (!user) return res.status(404).json({ message: "Người dùng không tồn tại" });

    if (user.password !== pwdInput) {
      console.log("Password mismatch: expected", user.password, "but got", pwdInput);
      return res.status(401).json({ message: "Mật khẩu hiện tại không chính xác" });
    }

    user.password = newPassword;
    await user.save();
    console.log("Password changed successfully");
    res.json({ message: "Đổi mật khẩu thành công" });
  } catch (error) {
    console.error("Change password error:", error);
    res.status(500).json({ message: "Lỗi server khi đổi mật khẩu" });
  }
});

// --- API WALLETS, CATEGORIES, TRANSACTIONS ---
app.get("/api/wallets", authenticateToken, async (req, res) => {
  res.json(await Wallet.find({ userId: req.user.id }));
});

app.post("/api/wallets", authenticateToken, async (req, res) => {
  const wallet = new Wallet({ ...req.body, userId: req.user.id });
  await wallet.save();
  res.status(201).json(wallet);
});

app.put("/api/wallets/:id", authenticateToken, async (req, res) => {
  const wallet = await Wallet.findOneAndUpdate({ _id: req.params.id, userId: req.user.id }, req.body, { new: true });
  res.json(wallet);
});

app.delete("/api/wallets/:id", authenticateToken, async (req, res) => {
  await Wallet.findOneAndDelete({ _id: req.params.id, userId: req.user.id });
  res.json({ message: "Xóa thành công" });
});

app.get("/api/categories", authenticateToken, async (req, res) => {
  res.json(await Category.find({ userId: req.user.id }));
});

app.post("/api/categories", authenticateToken, async (req, res) => {
  const category = new Category({ ...req.body, userId: req.user.id });
  await category.save();
  res.status(201).json(category);
});

app.put("/api/categories/:id", authenticateToken, async (req, res) => {
  const category = await Category.findOneAndUpdate({ _id: req.params.id, userId: req.user.id }, req.body, { new: true });
  res.json(category);
});

app.delete("/api/categories/:id", authenticateToken, async (req, res) => {
  await Category.findOneAndDelete({ _id: req.params.id, userId: req.user.id });
  res.json({ message: "Xóa thành công" });
});

app.get("/api/transactions", authenticateToken, async (req, res) => {
  res.json(await Transaction.find({ userId: req.user.id }));
});

app.post("/api/transactions", authenticateToken, async (req, res) => {
  const transaction = new Transaction({ ...req.body, userId: req.user.id });
  await transaction.save();
  res.status(201).json(transaction);
});

app.put("/api/transactions/:id", authenticateToken, async (req, res) => {
  const transaction = await Transaction.findOneAndUpdate({ _id: req.params.id, userId: req.user.id }, req.body, { new: true });
  res.json(transaction);
});

app.delete("/api/transactions/:id", authenticateToken, async (req, res) => {
  await Transaction.findOneAndDelete({ _id: req.params.id, userId: req.user.id });
  res.json({ message: "Xóa thành công" });
});

app.post("/api/wallets/transfer", authenticateToken, async (req, res) => {
  const { fromWalletId, toWalletId, amount, note } = req.body;
  const session = await mongoose.startSession();
  session.startTransaction();
  try {
    const fromW = await Wallet.findOne({ _id: fromWalletId, userId: req.user.id }).session(session);
    const toW = await Wallet.findOne({ _id: toWalletId, userId: req.user.id }).session(session);
    if (!fromW || !toW) throw new Error("Không tìm thấy ví");
    const amt = parseFloat(amount);
    const dateStr = new Date().toISOString().split("T")[0];
    await new Transaction({
      userId: req.user.id, walletId: fromWalletId, amount: amt,
      type: "EXPENSE", categoryName: "Chuyển tiền", description: note || `Chuyển đến ${toW.name}`, date: dateStr
    }).save({ session });
    await new Transaction({
      userId: req.user.id, walletId: toWalletId, amount: amt,
      type: "INCOME", categoryName: "Nhận tiền", description: note || `Nhận từ ${fromW.name}`, date: dateStr
    }).save({ session });
    fromW.balance -= amt;
    toW.balance += amt;
    await fromW.save({ session });
    await toW.save({ session });
    await session.commitTransaction();
    res.status(200).json({ message: "Chuyển tiền thành công" });
  } catch (error) {
    await session.abortTransaction();
    res.status(400).json({ message: error.message });
  } finally {
    session.endSession();
  }
});

const PORT = 3000;
app.listen(PORT, "0.0.0.0", () => {
  console.log(`Server is running at http://0.0.0.0:${PORT}`);
});
