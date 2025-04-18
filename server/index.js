const express = require('express');
const mongoose = require('mongoose');
const dotenv = require('dotenv');
const multer = require('multer');
const multerS3 = require('multer-s3');
const { S3Client, DeleteObjectCommand } = require('@aws-sdk/client-s3');
const { fromEnv } = require('@aws-sdk/credential-providers');
const crypto = require('crypto');

dotenv.config();

const app = express();
app.use(express.json());

// Kết nối tới MongoDB
mongoose.connect(process.env.connection_string)
    .then(() => console.log('Connected to MongoDB'))
    .catch((err) => console.error('MongoDB connection error:', err));

// Khởi tạo S3 Client với xử lý lỗi
let s3Client;
try {
    s3Client = new S3Client({
        credentials: {
            accessKeyId: process.env.AWS_ACCESS_KEY_ID, // Dùng trực tiếp
            secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY, // Dùng trực tiếp
        },
        endpoint: "https://fd0314cb84aca3240521990fc2bb803c.r2.cloudflarestorage.com",
        region: process.env.AWS_REGION || "auto",
    });
    console.log('S3 Client initialized successfully');
} catch (err) {
    console.error('Error initializing S3 Client:', err);
}

// Cấu hình Multer với multer-s3
const upload = multer({
    storage: multerS3({
        s3: s3Client,
        bucket: 'voca',
        metadata: (req, file, cb) => {
            console.log('Setting metadata for file:', file.originalname);
            cb(null, { fieldName: file.fieldname });
        },
        contentType: multerS3.AUTO_CONTENT_TYPE,
        key: (req, file, cb) => {
            const fileKey = `${Date.now().toString()}-${file.originalname}`;
            console.log('Generating key for file:', fileKey);
            cb(null, fileKey);
        },
    }),
}).single('file');

// File upload với xử lý lỗi
app.post('/upload', (req, res) => {
    console.log('Received upload request');
    console.log('Request body:', req.body);
    upload(req, res, async (err) => {
        if (err) {
            console.error('Multer upload error:', err);
            return res.status(500).json({ error: 'Upload failed', details: err.message });
        }
        if (!req.file) {
            console.warn('No file uploaded in request');
            return res.status(400).json({ error: 'No file uploaded' });
        }
        console.log('File uploaded successfully:', req.file.key);
        return res.status(200).json({
            filename: `https://pub-9baa3a81ecf34466aeb5591929ebf0b3.r2.dev/${req.file.key}`
        });
    });
});

// Delete file với xử lý lỗi
app.delete('/delete', async (req, res) => {
    console.log('Received delete request');
    console.log('Request body:', req.body);
    try {
        const deleteFile = {
            Bucket: 'voca',
            Key: decodeURI(req.body.key?.split('/').pop()),
        };
        console.log('File to delete:', deleteFile);

        const command = new DeleteObjectCommand(deleteFile);
        await s3Client.send(command);
        console.log('File deleted successfully:', deleteFile.Key);
        res.status(200).json({ message: 'File deleted successfully' });
    } catch (err) {
        console.error('Error deleting file:', err);
        res.status(500).json({ error: 'Failed to delete file', details: err.message });
    }
});

// Định nghĩa Schema
const userSchema = new mongoose.Schema({
    firebase_uid: { type: String, required: true, unique: true },
    fcmTokens: [{ type: String }],
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
    recorded_people: { type: Number, default: 0 },
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

const notificationSchema = new mongoose.Schema({
    recipient_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    sender_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    post_id: { type: mongoose.Schema.Types.ObjectId, ref: 'Post', required: true },
    type: { type: String, default: 'like' },
    is_read: { type: Boolean, default: false },
    created_at: { type: Date, default: Date.now }
});
const Notification = mongoose.model('Notification', notificationSchema);

const roomSchema = new mongoose.Schema({
    name: { type: String, required: true },
    code: { type: String, unique: true },
    is_private: { type: Boolean, default: false },
    description: { type: String },
    created_at: { type: Date, default: Date.now },
    updated_at: { type: Date, default: Date.now },
    created_by: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
    members: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }],
    queue: [{ type: mongoose.Schema.Types.ObjectId, ref: 'Song' }],
    current_song: { type: mongoose.Schema.Types.ObjectId, ref: 'Song' },
    current_song_start_time: { type: Date },
    chats: [{
        message_type: { type: String },
        user_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
        message: { type: String },
        timestamp: { type: Date, default: Date.now },
    }],
});

roomSchema.pre('save', async function(next) {
    if (!this.code) {
        let generatedCode = generateRandomCode();
        while (await Room.findOne({ code: generatedCode })) {
            generatedCode = generateRandomCode();
        }
        this.code = generatedCode;
        console.log('Generated room code:', generatedCode);
    }
    next();
});

function generateRandomCode() {
    return crypto.randomBytes(3).toString('hex').toUpperCase();
}

const Room = mongoose.model('Room', roomSchema);

// CRUD APIs for Rooms
app.post('/rooms', async (req, res) => {
    try {
        let room = new Room(req.body);
        await room.save();
        room = await Room.findById(room._id)
            .populate('created_by')
            .populate('members')
            .populate('queue')
            .populate('current_song');
        console.log('Room created:', room);
        res.status(201).json(room);
    } catch (err) {
        console.error('Error creating room:', err);
        res.status(400).json({ error: 'Failed to create room', details: err.message });
    }
});

app.get('/rooms/', async (req, res) => {
    try {
        let filter = {};
        if (req.query.name) filter.name = new RegExp(req.query.name, 'i');
        if (req.query.code) filter.code = req.query.code;
        if (req.query.user_id) filter.created_by = new mongoose.Types.ObjectId(req.query.user_id);

        const room = req.query.code
            ? await Room.findOne(filter).populate('created_by').populate('members').populate('queue').populate('current_song')
            : await Room.find(filter).populate('created_by').populate('members').populate('queue').populate('current_song');
        console.log('Rooms found:', room);
        room ? res.json(room) : res.status(404).json({ error: 'Room not found' });
    } catch (err) {
        console.error('Error fetching rooms:', err);
        res.status(500).json({ error: 'Failed to fetch rooms', details: err.message });
    }
});

app.get('/rooms/:id', async (req, res) => {
    try {
        const room = await Room.findById(req.params.id)
            .populate('created_by')
            .populate('members')
            .populate('queue')
            .populate('current_song');
        room ? res.json(room) : res.status(404).json({ error: 'Room not found' });
    } catch (err) {
        console.error('Error fetching room:', err);
        res.status(500).json({ error: 'Failed to fetch room', details: err.message });
    }
});

app.delete('/rooms/:id', async (req, res) => {
    try {
        const room = await Room.findByIdAndDelete(req.params.id);
        if (!room) return res.status(404).json({ error: 'Room not found' });
        console.log('Room deleted:', req.params.id);
        res.json({ message: 'Room deleted' });
    } catch (err) {
        console.error('Error deleting room:', err);
        res.status(500).json({ error: 'Failed to delete room', details: err.message });
    }
});

app.put('/rooms/:id', async (req, res) => {
    try {
        const room = await Room.findByIdAndUpdate(req.params.id, req.body, { new: true });
        if (!room) return res.status(404).json({ error: 'Room not found' });
        console.log('Room updated:', room);
        res.json(room);
    } catch (err) {
        console.error('Error updating room:', err);
        res.status(400).json({ error: 'Failed to update room', details: err.message });
    }
});

// CRUD APIs for Notifications
app.post('/likes', async (req, res) => {
    let like; 
    try {
        const { post_id, user_id } = req.body;

        if (!post_id || !user_id) {
            return res.status(400).json({ error: 'post_id and user_id are required' });
        }

        const post = await Post.findById(post_id);
        if (!post) {
            return res.status(404).json({ error: 'Post not found' });
        }

        const existingLike = await Like.findOne({ post_id, user_id });
        if (existingLike) {
            return res.status(400).json({ error: 'User already liked this post' });
        }

        like = new Like({ post_id, user_id });
        await like.save();

        await Post.findByIdAndUpdate(post_id, { $inc: { likes: 1 } }, { new: true });

        if (String(post.user_id) !== String(user_id)) {
            const notification = new Notification({
                recipient_id: post.user_id,
                sender_id: user_id,
                post_id,
                type: 'like',
                is_read: false
            });
            await notification.save();

            await sendPushNotification(post.user_id, post_id);
        }

        const populatedLike = await Like.findById(like._id)
            .populate('user_id')
            .populate({ path: 'post_id', populate: [{ path: 'user_id' }, { path: 'song_id', populate: { path: 'uploaded_by' } }] });
        console.log('Like created:', populatedLike);
        res.status(201).json(populatedLike);
    } catch (err) {
        console.error('Error creating like:', err);
        if (like && like._id) {
            await Like.findByIdAndDelete(like._id);
        }
        res.status(400).json({ error: 'Failed to create like', details: err.message });
    }
});

app.delete('/likes/:id', async (req, res) => {
    try {
        const like = await Like.findByIdAndDelete(req.params.id);
        if (!like) return res.status(404).json({ error: 'Like not found' });
        const post = await Post.findByIdAndUpdate(like.post_id, { $inc: { likes: -1 } }, { new: true });
        if (!post) return res.status(404).json({ error: 'Post not found' });
        if (post.likes < 0) await Post.findByIdAndUpdate(like.post_id, { likes: 0 });
        console.log('Like deleted:', req.params.id);
        res.json({ message: 'Like deleted', post_likes: post.likes });
    } catch (err) {
        console.error('Error deleting like:', err);
        res.status(500).json({ error: 'Failed to delete like', details: err.message });
    }
});

app.get('/notifications/:userId', async (req, res) => {
    try {
        const notifications = await Notification.find({ recipient_id: req.params.userId })
            .populate('recipient_id')
            .populate('sender_id')
            .populate({ path: 'post_id', populate: [{ path: 'user_id' }, { path: 'song_id', populate: { path: 'uploaded_by' } }] })
            .sort({ created_at: -1 });
        console.log('Notifications fetched for user:', req.params.userId);
        res.json(notifications);
    } catch (err) {
        console.error('Error fetching notifications:', err);
        res.status(400).json({ error: 'Failed to fetch notifications', details: err.message });
    }
});

app.put('/notifications/:id/read', async (req, res) => {
    try {
        const notification = await Notification.findByIdAndUpdate(req.params.id, { is_read: true }, { new: true })
            .populate('recipient_id')
            .populate('sender_id')
            .populate({ path: 'post_id', populate: [{ path: 'user_id' }, { path: 'song_id', populate: { path: 'uploaded_by' } }] });
        if (!notification) return res.status(404).json({ error: 'Notification not found' });
        console.log('Notification marked as read:', req.params.id);
        res.json(notification);
    } catch (err) {
        console.error('Error updating notification:', err);
        res.status(400).json({ error: 'Failed to update notification', details: err.message });
    }
});

app.delete('/notifications/:id', async (req, res) => {
    try {
        const notification = await Notification.findByIdAndDelete(req.params.id);
        if (!notification) return res.status(404).json({ error: 'Notification not found' });
        console.log('Notification deleted:', req.params.id);
        res.json({ message: 'Notification deleted' });
    } catch (err) {
        console.error('Error deleting notification:', err);
        res.status(400).json({ error: 'Failed to delete notification', details: err.message });
    }
});

// API Endpoints cho User
app.post('/users', async (req, res) => {
    try {
        const user = new User(req.body);
        await user.save();
        console.log('User created:', user);
        res.status(201).json(user);
    } catch (err) {
        console.error('Error creating user:', err);
        res.status(400).json({ error: 'Failed to create user', details: err.message });
    }
});

app.get('/users', async (req, res) => {
    try {
        const filters = { ...req.query };
        const users = await User.find(filters);
        console.log('Users fetched:', users.length);
        res.json(users);
    } catch (err) {
        console.error('Error fetching users:', err);
        res.status(500).json({ error: 'Failed to fetch users', details: err.message });
    }
});

app.get('/users/:id', async (req, res) => {
    try {
        const user = await User.findById(req.params.id);
        if (!user) return res.status(404).json({ error: 'User not found' });
        res.json(user);
    } catch (err) {
        console.error('Error fetching user:', err);
        res.status(500).json({ error: 'Failed to fetch user', details: err.message });
    }
});

app.put('/users/:id', async (req, res) => {
    try {
        const user = await User.findByIdAndUpdate(req.params.id, req.body, { new: true });
        if (!user) return res.status(404).json({ error: 'User not found' });
        console.log('User updated:', user);
        res.json(user);
    } catch (err) {
        console.error('Error updating user:', err);
        res.status(400).json({ error: 'Failed to update user', details: err.message });
    }
});

app.delete('/users/:id', async (req, res) => {
    try {
        const user = await User.findByIdAndDelete(req.params.id);
        if (!user) return res.status(404).json({ error: 'User not found' });
        console.log('User deleted:', req.params.id);
        res.json({ message: 'User deleted' });
    } catch (err) {
        console.error('Error deleting user:', err);
        res.status(500).json({ error: 'Failed to delete user', details: err.message });
    }
});

app.patch('/users/:id/fcm-token', async (req, res) => {
    try {
        const { id } = req.params;
        const { fcmToken } = req.body;

        if (!fcmToken) {
            return res.status(400).json({ error: 'fcmToken is required' });
        }

        const user = await User.findById(id);
        if (!user) {
            return res.status(404).json({ error: 'User not found' });
        }

        if (!user.fcmTokens) {
            user.fcmTokens = [];
        }

        if (!user.fcmTokens.includes(fcmToken)) {
            user.fcmTokens.push(fcmToken);
            await user.save();
        }

        console.log('FCM token updated for user:', id);
        res.json(user);
    } catch (error) {
        console.error('Error updating FCM token:', error);
        res.status(500).json({ error: 'Failed to update FCM token', details: error.message });
    }
});

// API Endpoints cho Song
app.post('/songs', async (req, res) => {
    try {
        const song = new Song(req.body);
        await song.save();
        const populatedSong = await Song.findById(song._id).populate('uploaded_by');
        console.log('Song created:', populatedSong);
        res.status(201).json(populatedSong);
    } catch (err) {
        console.error('Error creating song:', err);
        res.status(400).json({ error: 'Failed to create song', details: err.message });
    }
});

app.get('/songs', async (req, res) => {
    try {
        const filters = { ...req.query };
        const songs = await Song.find(filters).populate('uploaded_by');
        console.log('Songs fetched:', songs.length);
        res.json(songs);
    } catch (err) {
        console.error('Error fetching songs:', err);
        res.status(500).json({ error: 'Failed to fetch songs', details: err.message });
    }
});

app.get('/api/songs', async (req, res) => {
    try {
        const { title } = req.query;
        let filter = {};
        if (title) filter.title = { $regex: `.*${title}.*`, $options: 'i' };
        const songs = await Song.find(filter).populate('uploaded_by');
        console.log('Songs searched:', songs.length);
        res.json(songs);
    } catch (err) {
        console.error('Error searching songs:', err);
        res.status(500).json({ error: 'Failed to search songs', details: err.message });
    }
});

app.get('/songs/:id', async (req, res) => {
    try {
        const song = await Song.findById(req.params.id).populate('uploaded_by');
        if (!song) return res.status(404).json({ error: 'Song not found' });
        res.json(song);
    } catch (err) {
        console.error('Error fetching song:', err);
        res.status(500).json({ error: 'Failed to fetch song', details: err.message });
    }
});

app.put('/songs/:id', async (req, res) => {
    try {
        const song = await Song.findByIdAndUpdate(req.params.id, req.body, { new: true }).populate('uploaded_by');
        if (!song) return res.status(404).json({ error: 'Song not found' });
        console.log('Song updated:', song);
        res.json(song);
    } catch (err) {
        console.error('Error updating song:', err);
        res.status(400).json({ error: 'Failed to update song', details: err.message });
    }
});

app.delete('/songs/:id', async (req, res) => {
    try {
        const song = await Song.findByIdAndDelete(req.params.id);
        if (!song) return res.status(404).json({ error: 'Song not found' });
        console.log('Song deleted:', req.params.id);
        res.json({ message: 'Song deleted' });
    } catch (err) {
        console.error('Error deleting song:', err);
        res.status(500).json({ error: 'Failed to delete song', details: err.message });
    }
});

// API Endpoints cho Post
app.post('/posts', async (req, res) => {
    try {
        const post = new Post(req.body);
        await post.save();
        const populatedPost = await Post.findById(post._id)
            .populate('user_id')
            .populate({ path: 'song_id', populate: { path: 'uploaded_by' } });
        console.log('Post created:', populatedPost);
        res.status(201).json(populatedPost);
    } catch (err) {
        console.error('Error creating post:', err);
        res.status(400).json({ error: 'Failed to create post', details: err.message });
    }
});

app.get('/posts', async (req, res) => {
    try {
        const filters = { ...req.query };
        const posts = await Post.find(filters)
            .populate('user_id')
            .populate({ path: 'song_id', populate: { path: 'uploaded_by' } });
        console.log('Posts fetched:', posts.length);
        res.json(posts);
    } catch (err) {
        console.error('Error fetching posts:', err);
        res.status(500).json({ error: 'Failed to fetch posts', details: err.message });
    }
});

app.get('/posts/:id', async (req, res) => {
    try {
        const post = await Post.findById(req.params.id)
            .populate('user_id')
            .populate({ path: 'song_id', populate: { path: 'uploaded_by' } });
        if (!post) return res.status(404).json({ error: 'Post not found' });
        res.json(post);
    } catch (err) {
        console.error('Error fetching post:', err);
        res.status(500).json({ error: 'Failed to fetch post', details: err.message });
    }
});

app.put('/posts/:id', async (req, res) => {
    try {
        const post = await Post.findByIdAndUpdate(req.params.id, req.body, { new: true })
            .populate('user_id')
            .populate({ path: 'song_id', populate: { path: 'uploaded_by' } });
        if (!post) return res.status(404).json({ error: 'Post not found' });
        console.log('Post updated:', post);
        res.json(post);
    } catch (err) {
        console.error('Error updating post:', err);
        res.status(400).json({ error: 'Failed to update post', details: err.message });
    }
});

app.delete('/posts/:id', async (req, res) => {
    try {
        const post = await Post.findByIdAndDelete(req.params.id);
        if (!post) return res.status(404).json({ error: 'Post not found' });
        console.log('Post deleted:', req.params.id);
        res.json({ message: 'Post deleted' });
    } catch (err) {
        console.error('Error deleting post:', err);
        res.status(500).json({ error: 'Failed to delete post', details: err.message });
    }
});

// API Endpoints cho Like
app.get('/likes', async (req, res) => {
    try {
        const filters = { ...req.query };
        const likes = await Like.find(filters)
            .populate('user_id')
            .populate({ path: 'post_id', populate: [{ path: 'user_id' }, { path: 'song_id', populate: { path: 'uploaded_by' } }] });
        console.log('Likes fetched:', likes.length);
        res.json(likes);
    } catch (err) {
        console.error('Error fetching likes:', err);
        res.status(500).json({ error: 'Failed to fetch likes', details: err.message });
    }
});

app.get('/likes/:id', async (req, res) => {
    try {
        const like = await Like.findById(req.params.id)
            .populate('user_id')
            .populate({ path: 'post_id', populate: [{ path: 'user_id' }, { path: 'song_id', populate: { path: 'uploaded_by' } }] });
        if (!like) return res.status(404).json({ error: 'Like not found' });
        res.json(like);
    } catch (err) {
        console.error('Error fetching like:', err);
        res.status(500).json({ error: 'Failed to fetch like', details: err.message });
    }
});

app.put('/likes/:id', async (req, res) => {
    try {
        const like = await Like.findByIdAndUpdate(req.params.id, req.body, { new: true })
            .populate('user_id')
            .populate({ path: 'post_id', populate: [{ path: 'user_id' }, { path: 'song_id', populate: { path: 'uploaded_by' } }] });
        if (!like) return res.status(404).json({ error: 'Like not found' });
        console.log('Like updated:', like);
        res.json(like);
    } catch (err) {
        console.error('Error updating like:', err);
        res.status(400).json({ error: 'Failed to update like', details: err.message });
    }
});

app.post('/posts/:id/like', async (req, res) => {
    try {
        const postId = req.params.id;
        const { user_id } = req.body;
        const post = await Post.findById(postId);
        if (!post) return res.status(404).json({ error: 'Post not found' });
        const existingLike = await Like.findOne({ post_id: postId, user_id });
        if (existingLike) return res.status(400).json({ error: 'User already liked this post' });
        const like = new Like({ post_id: postId, user_id });
        await like.save();
        post.likes += 1;
        await post.save();
        const populatedLike = await Like.findById(like._id)
            .populate('user_id')
            .populate({ path: 'post_id', populate: [{ path: 'user_id' }, { path: 'song_id', populate: { path: 'uploaded_by' } }] });
        console.log('Post liked:', populatedLike);
        res.status(201).json(populatedLike);
    } catch (err) {
        console.error('Error liking post:', err);
        res.status(400).json({ error: 'Failed to like post', details: err.message });
    }
});

app.delete('/posts/:id/unlike', async (req, res) => {
    try {
        const postId = req.params.id;
        const { user_id } = req.body;
        const post = await Post.findById(postId);
        if (!post) return res.status(404).json({ error: 'Post not found' });
        const like = await Like.findOneAndDelete({ post_id: postId, user_id });
        if (!like) return res.status(404).json({ error: 'Like not found' });
        if (post.likes > 0) {
            post.likes -= 1;
            await post.save();
        }
        console.log('Post unliked:', postId);
        res.json({ message: 'Post unliked successfully', likes: post.likes });
    } catch (err) {
        console.error('Error unliking post:', err);
        res.status(400).json({ error: 'Failed to unlike post', details: err.message });
    }
});

app.post('/songs/:id/record', async (req, res) => {
    try {
        const songId = req.params.id;
        const updatedSong = await Song.findByIdAndUpdate(songId, { $inc: { recorded_people: 1 } }, { new: true })
            .populate('uploaded_by');
        if (!updatedSong) return res.status(404).json({ error: 'Song not found' });
        console.log('Song recorded:', updatedSong);
        res.json(updatedSong);
    } catch (err) {
        console.error('Error recording song:', err);
        res.status(400).json({ error: 'Failed to record song', details: err.message });
    }
});

const admin = require('firebase-admin');

const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT_KEY);

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

async function sendPushNotification(user_id, post_id) {
    try {
        const user = await User.findById(user_id);
        if (!user || !user.fcmTokens || user.fcmTokens.length === 0) {
            console.log('No FCM tokens found for user_id:', user_id);
            return;
        }

        const messages = user.fcmTokens.map(token => ({
            token,
            notification: {
                title: `${user.username} đã thích bài viết của bạn`,
                body: `Bài viết của bạn đã nhận được một lượt thích mới.`,
            },
            data: {
                post_id: post_id.toString(),
            }
        }));

        const results = await Promise.all(messages.map(message =>
            admin.messaging().send(message).catch(err => ({ error: err }))
        ));

        const invalidTokens = results
            .map((result, index) => result.error && result.error.code === 'messaging/registration-token-not-registered' ? user.fcmTokens[index] : null)
            .filter(token => token);

        if (invalidTokens.length > 0) {
            user.fcmTokens = user.fcmTokens.filter(t => !invalidTokens.includes(t));
            await user.save();
            console.log('Removed invalid tokens:', invalidTokens);
        }

        console.log('Push notifications sent successfully to:', user.fcmTokens);
    } catch (err) {
        console.error('Error sending push notification:', err);
    }
}

// Khởi động server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});