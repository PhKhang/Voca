const express = require('express');
const mongoose = require('mongoose');
const dotenv = require('dotenv');
dotenv.config();

const app = express();
app.use(express.json());

// Kết nối tới MongoDB
mongoose.connect(process.env.connection_string)
    .then(() => console.log('Connected to MongoDB'))
    .catch((err) => console.log('Error:', err));

// Định nghĩa Schema
const userSchema = new mongoose.Schema({
    firebase_uid: { type: String, required: true, unique: true },
    username: { type: String, required: true },
    email: { type: String, required: true, unique: true },
    avatar: { type: String },
    roll: { type: String, default: 'user' }, // Có thể bạn muốn sửa "roll" thành "role"
    created_at: { type: Date, default: Date.now },
    updated_at: { type: Date, default: Date.now }
});
const User = mongoose.model('User', userSchema);

const songSchema = new mongoose.Schema({
    youtube_id: { type: String, required: true, unique: true },
    title: { type: String, required: true },
    thumbnail: { type: String, required: true },
    uploaded_by: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    created_at: { type: Date, default: Date.now }
});
const Song = mongoose.model('Song', songSchema);

const postSchema = new mongoose.Schema({
    user_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    song_id: { type: mongoose.Schema.Types.ObjectId, ref: 'Song', required: true },
    audio_url: { type: String, required: true },
    caption: { type: String },
    likes: { type: Number, default: 0 },
    created_at: { type: Date, default: Date.now }
});
const Post = mongoose.model('Post', postSchema);

const likeSchema = new mongoose.Schema({
    post_id: { type: mongoose.Schema.Types.ObjectId, ref: 'Post', required: true },
    user_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    created_at: { type: Date, default: Date.now }
});
const Like = mongoose.model('Like', likeSchema);

// API Endpoints
// Lấy danh sách users
app.get('/users', async (req, res) => {
    try {
        const filters = { ...req.query };
        const users = await User.find(filters);
        res.json(users);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Lấy danh sách songs với thông tin uploaded_by
app.get('/songs', async (req, res) => {
    try {
        const filters = { ...req.query };
        const songs = await Song.find(filters).populate('uploaded_by');
        res.json(songs);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Lấy danh sách posts với thông tin user_id và song_id (bao gồm uploaded_by trong song)
app.get('/posts', async (req, res) => {
    try {
        const filters = { ...req.query };
        const posts = await Post.find(filters)
            .populate('user_id')
            .populate({
                path: 'song_id',
                populate: { path: 'uploaded_by' }
            });
        res.json(posts);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Lấy danh sách likes với thông tin user_id và post_id (bao gồm user_id và song_id trong post)
app.get('/likes', async (req, res) => {
    try {
        const filters = { ...req.query };
        const likes = await Like.find(filters)
            .populate('user_id')
            .populate({
                path: 'post_id',
                populate: [
                    { path: 'user_id' },
                    {
                        path: 'song_id',
                        populate: { path: 'uploaded_by' }
                    }
                ]
            });
        res.json(likes);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Khởi động server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});