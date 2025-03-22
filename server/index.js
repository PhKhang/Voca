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
    role: { type: String, default: 'user' }, 
    created_at: { type: Date, default: Date.now },
    updated_at: { type: Date, default: Date.now }
});
const User = mongoose.model('User', userSchema);

const songSchema = new mongoose.Schema({
    youtube_id: { type: String, required: true, unique: true },
    title: { type: String, required: true },
    thumbnail: { type: String, required: true },
    mp3_file: { type: String, required: true },
    uploaded_by: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    recorded_people: {type: Number, default: 0},
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

// API Endpoints cho User
// Create User
app.post('/users', async (req, res) => {
    try {
        const user = new User(req.body);
        await user.save();
        res.status(201).json(user);
    } catch (err) {
        res.status(400).json({ error: err.message });
    }
});

// Read Users (đã có)
app.get('/users', async (req, res) => {
    try {
        const filters = { ...req.query };
        const users = await User.find(filters);
        res.json(users);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Read User by ID
app.get('/users/:id', async (req, res) => {
    try {
        const user = await User.findById(req.params.id);
        if (!user) return res.status(404).json({ error: 'User not found' });
        res.json(user);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Update User
app.put('/users/:id', async (req, res) => {
    try {
        const user = await User.findByIdAndUpdate(req.params.id, req.body, { new: true });
        if (!user) return res.status(404).json({ error: 'User not found' });
        res.json(user);
    } catch (err) {
        res.status(400).json({ error: err.message });
    }
});

// Delete User
app.delete('/users/:id', async (req, res) => {
    try {
        const user = await User.findByIdAndDelete(req.params.id);
        if (!user) return res.status(404).json({ error: 'User not found' });
        res.json({ message: 'User deleted' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// API Endpoints cho Song
// Create Song
app.post('/songs', async (req, res) => {
    try {
        const song = new Song(req.body);
        await song.save();
        const populatedSong = await Song.findById(song._id).populate('uploaded_by');
        res.status(201).json(populatedSong);
    } catch (err) {
        res.status(400).json({ error: err.message });
    }
});

// Read Songs (đã có)
app.get('/songs', async (req, res) => {
    try {
        const filters = { ...req.query };
        const songs = await Song.find(filters).populate('uploaded_by');
        res.json(songs);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Read Song by ID
app.get('/songs/:id', async (req, res) => {
    try {
        const song = await Song.findById(req.params.id).populate('uploaded_by');
        if (!song) return res.status(404).json({ error: 'Song not found' });
        res.json(song);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Update Song
app.put('/songs/:id', async (req, res) => {
    try {
        const song = await Song.findByIdAndUpdate(req.params.id, req.body, { new: true }).populate('uploaded_by');
        if (!song) return res.status(404).json({ error: 'Song not found' });
        res.json(song);
    } catch (err) {
        res.status(400).json({ error: err.message });
    }
});

// Delete Song
app.delete('/songs/:id', async (req, res) => {
    try {
        const song = await Song.findByIdAndDelete(req.params.id);
        if (!song) return res.status(404).json({ error: 'Song not found' });
        res.json({ message: 'Song deleted' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Search Song by title
app.get('/songs', async (req, res) => {
    const { title } = req.query;
    let filter = {};
    if (title) {
        filter.title = { $regex: `.*${title}.*`, $options: 'i' };
    }
    const songs = await Song.find(filter).populate('uploaded_by');
    res.json(songs);
});

// API Endpoints cho Post
// Create Post
app.post('/posts', async (req, res) => {
    try {
        const post = new Post(req.body);
        await post.save();
        const populatedPost = await Post.findById(post._id)
            .populate('user_id')
            .populate({ path: 'song_id', populate: { path: 'uploaded_by' } });
        res.status(201).json(populatedPost);
    } catch (err) {
        res.status(400).json({ error: err.message });
    }
});

// Read Posts (đã có)
app.get('/posts', async (req, res) => {
    try {
        const filters = { ...req.query };
        const posts = await Post.find(filters)
            .populate('user_id')
            .populate({ path: 'song_id', populate: { path: 'uploaded_by' } });
        res.json(posts);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Read Post by ID
app.get('/posts/:id', async (req, res) => {
    try {
        const post = await Post.findById(req.params.id)
            .populate('user_id')
            .populate({ path: 'song_id', populate: { path: 'uploaded_by' } });
        if (!post) return res.status(404).json({ error: 'Post not found' });
        res.json(post);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Update Post
app.put('/posts/:id', async (req, res) => {
    try {
        const post = await Post.findByIdAndUpdate(req.params.id, req.body, { new: true })
            .populate('user_id')
            .populate({ path: 'song_id', populate: { path: 'uploaded_by' } });
        if (!post) return res.status(404).json({ error: 'Post not found' });
        res.json(post);
    } catch (err) {
        res.status(400).json({ error: err.message });
    }
});

// Delete Post
app.delete('/posts/:id', async (req, res) => {
    try {
        const post = await Post.findByIdAndDelete(req.params.id);
        if (!post) return res.status(404).json({ error: 'Post not found' });
        res.json({ message: 'Post deleted' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// API Endpoints cho Like
// Create Like
app.post('/likes', async (req, res) => {
    try {
        const like = new Like(req.body);
        await like.save();
        const populatedLike = await Like.findById(like._id)
            .populate('user_id')
            .populate({
                path: 'post_id',
                populate: [
                    { path: 'user_id' },
                    { path: 'song_id', populate: { path: 'uploaded_by' } }
                ]
            });
        res.status(201).json(populatedLike);
    } catch (err) {
        res.status(400).json({ error: err.message });
    }
});

// Read Likes (đã có)
app.get('/likes', async (req, res) => {
    try {
        const filters = { ...req.query };
        const likes = await Like.find(filters)
            .populate('user_id')
            .populate({
                path: 'post_id',
                populate: [
                    { path: 'user_id' },
                    { path: 'song_id', populate: { path: 'uploaded_by' } }
                ]
            });
        res.json(likes);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Read Like by ID
app.get('/likes/:id', async (req, res) => {
    try {
        const like = await Like.findById(req.params.id)
            .populate('user_id')
            .populate({
                path: 'post_id',
                populate: [
                    { path: 'user_id' },
                    { path: 'song_id', populate: { path: 'uploaded_by' } }
                ]
            });
        if (!like) return res.status(404).json({ error: 'Like not found' });
        res.json(like);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Update Like
app.put('/likes/:id', async (req, res) => {
    try {
        const like = await Like.findByIdAndUpdate(req.params.id, req.body, { new: true })
            .populate('user_id')
            .populate({
                path: 'post_id',
                populate: [
                    { path: 'user_id' },
                    { path: 'song_id', populate: { path: 'uploaded_by' } }
                ]
            });
        if (!like) return res.status(404).json({ error: 'Like not found' });
        res.json(like);
    } catch (err) {
        res.status(400).json({ error: err.message });
    }
});

// Delete Like
app.delete('/likes/:id', async (req, res) => {
    try {
        const like = await Like.findByIdAndDelete(req.params.id);
        if (!like) return res.status(404).json({ error: 'Like not found' });
        res.json({ message: 'Like deleted' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Like a Post
app.post('/posts/:id/like', async (req, res) => {
    try {
        const postId = req.params.id;
        const { user_id } = req.body; // Giả sử user_id được gửi trong body

        // Kiểm tra xem post có tồn tại không
        const post = await Post.findById(postId);
        if (!post) return res.status(404).json({ error: 'Post not found' });

        // Kiểm tra xem user đã like chưa (tránh like trùng)
        const existingLike = await Like.findOne({ post_id: postId, user_id });
        if (existingLike) return res.status(400).json({ error: 'User already liked this post' });

        // Tạo bản ghi Like
        const like = new Like({ post_id: postId, user_id });
        await like.save();

        // Tăng số likes trong Post
        post.likes += 1;
        await post.save();

        // Populate dữ liệu trả về
        const populatedLike = await Like.findById(like._id)
            .populate('user_id')
            .populate({
                path: 'post_id',
                populate: [
                    { path: 'user_id' },
                    { path: 'song_id', populate: { path: 'uploaded_by' } }
                ]
            });

        res.status(201).json(populatedLike);
    } catch (err) {
        res.status(400).json({ error: err.message });
    }
});

// Unlike a Post
app.delete('/posts/:id/unlike', async (req, res) => {
    try {
        const postId = req.params.id;
        const { user_id } = req.body; // Giả sử user_id được gửi trong body

        // Kiểm tra xem post có tồn tại không
        const post = await Post.findById(postId);
        if (!post) return res.status(404).json({ error: 'Post not found' });

        // Tìm và xóa bản ghi Like
        const like = await Like.findOneAndDelete({ post_id: postId, user_id });
        if (!like) return res.status(404).json({ error: 'Like not found' });

        // Giảm số likes trong Post
        if (post.likes > 0) {
            post.likes -= 1;
            await post.save();
        }

        res.json({ message: 'Post unliked successfully', likes: post.likes });
    } catch (err) {
        res.status(400).json({ error: err.message });
    }
});

// Increment recorded_people for a Song
app.post('/songs/:id/record', async (req, res) => {
    try {
        const songId = req.params.id;

        // Kiểm tra xem song có tồn tại không
        const song = await Song.findById(songId);
        if (!song) return res.status(404).json({ error: 'Song not found' });

        // Tăng recorded_people lên 1 bằng $inc
        const updatedSong = await Song.findByIdAndUpdate(
            songId,
            { $inc: { recorded_people: 1 } }, // Tăng giá trị recorded_people lên 1
            { new: true } // Trả về document sau khi cập nhật
        ).populate('uploaded_by');

        res.json(updatedSong);
    } catch (err) {
        res.status(400).json({ error: err.message });
    }
});

// Khởi động server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});