const express = require('express');
const mongoose = require('mongoose');
const dotenv = require('dotenv');
dotenv.config();

const app = express();
app.use(express.json());
mongoose.connect(process.env.connection_string)
    .then(() => console.log('Connected to MongoDB'))
    .catch((err) => console.log('Error:', err));

// Schema Definitions
const userSchema = new mongoose.Schema({
    firebase_uid: { type: String, required: true, unique: true },
    username: { type: String, required: true },
    email: { type: String, required: true, unique: true },
    avatar: { type: String },
    roll: { type: String, default: 'user' },
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

// CRUD APIs for Users
app.post('/users', async (req, res) => {
    try {
        const user = new User(req.body);
        await user.save();
        res.status(201).json(user);
    } catch (err) {
        res.status(400).json({ error: err.message });
    }
});

app.get('/users', async (req, res) => {
    const users = await User.find();
    res.json(users);
});

app.get('/users/:id', async (req, res) => {
    const user = await User.findById(req.params.id);
    user ? res.json(user) : res.status(404).json({ error: 'User not found' });
});

app.put('/users/:id', async (req, res) => {
    try {
        const user = await User.findByIdAndUpdate(req.params.id, req.body, { new: true });
        res.json(user);
    } catch (err) {
        res.status(400).json({ error: err.message });
    }
});

app.delete('/users/:id', async (req, res) => {
    await User.findByIdAndDelete(req.params.id);
    res.json({ message: 'User deleted' });
});

// CRUD APIs for Songs
app.post('/songs', async (req, res) => {
    try {
        const song = new Song(req.body);
        await song.save();
        res.status(201).json(song);
    } catch (err) {
        res.status(400).json({ error: err.message });
    }
});

app.get('/songs', async (req, res) => {
    const songs = await Song.find().populate('uploaded_by');
    res.json(songs);
});

app.get('/songs/:id', async (req, res) => {
    const song = await Song.findById(req.params.id).populate('uploaded_by');
    song ? res.json(song) : res.status(404).json({ error: 'Song not found' });
});

app.delete('/songs/:id', async (req, res) => {
    await Song.findByIdAndDelete(req.params.id);
    res.json({ message: 'Song deleted' });
});

// CRUD APIs for Posts
app.post('/posts', async (req, res) => {
    try {
        const post = new Post(req.body);
        await post.save();
        res.status(201).json(post);
    } catch (err) {
        res.status(400).json({ error: err.message });
    }
});

app.get('/posts', async (req, res) => {
    const posts = await Post.find().populate('user_id').populate('song_id');
    res.json(posts);
});

app.get('/posts/:id', async (req, res) => {
    const post = await Post.findById(req.params.id).populate('user_id').populate('song_id');
    post ? res.json(post) : res.status(404).json({ error: 'Post not found' });
});

app.delete('/posts/:id', async (req, res) => {
    await Post.findByIdAndDelete(req.params.id);
    res.json({ message: 'Post deleted' });
});

// CRUD APIs for Likes
app.post('/likes', async (req, res) => {
    try {
        const like = new Like(req.body);
        await like.save();
        res.status(201).json(like);
    } catch (err) {
        res.status(400).json({ error: err.message });
    }
});

app.get('/likes', async (req, res) => {
    const likes = await Like.find().populate('post_id').populate('user_id');
    res.json(likes);
});

app.delete('/likes/:id', async (req, res) => {
    await Like.findByIdAndDelete(req.params.id);
    res.json({ message: 'Like removed' });
});

app.listen(3000, () => console.log('Server is running on port 3000'));
