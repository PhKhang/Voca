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
    .catch((err) => console.log('Error:', err));

const s3Client = new S3Client({
    credentials: fromEnv(),
    endpoint: "https://fd0314cb84aca3240521990fc2bb803c.r2.cloudflarestorage.com",
});
    
const upload = multer({
    storage: multerS3({
        s3: s3Client,
        bucket: 'voca',
        metadata: (req, file, cb) => {
            cb(null, { fieldName: file.fieldname });
        },
        contentType: multerS3.AUTO_CONTENT_TYPE,
        key: (req, file, cb) => {
            cb(null, `${Date.now().toString()}-${file.originalname}`);
        },
    }),
});

// File upload
app.post('/upload', upload.single('file'), async (req, res) => {
    if (!req.file) {
        return res.status(400).send('No file uploaded.');
    }
    console.log(req.file.key);
    return res.status(200).send({ filename: `https://pub-9baa3a81ecf34466aeb5591929ebf0b3.r2.dev/${req.file.key}` });
}); 

app.delete('/delete', async (req, res) => {
    console.log("In");
    console.log("Request body:", req.body);
    const deleteFile = {
        "Bucket": "voca",
        "Key": decodeURI(req.body.key?.split('/').pop()),
    }
    console.log("To delete:", deleteFile);
    
    const command = new DeleteObjectCommand(deleteFile);
    await s3Client.send(command);
    res.status(200).send('File deleted successfully');
});
    
// Schema Definitions
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
    chats: [
        {
            message_type: { type: String },
            user_id: { type: mongoose.Schema.Types.ObjectId, ref: 'User' },
            message: { type: String },
            timestamp: { type: Date, default: Date.now },
        },
    ],
});
// Pre-save hook to generate code
roomSchema.pre('save', async function(next) {
    if (!this.code) {
        // Generate a 6-character alphanumeric code
        let generatedCode = generateRandomCode();

        // Ensure that the generated code is unique
        while (await Room.findOne({ code: generatedCode })) {
            generatedCode = generateRandomCode();  // Regenerate the code if it's already taken
        }

        this.code = generatedCode;
    }
    next();
});

// Random 6-character alphanumeric code
function generateRandomCode() {
    return crypto.randomBytes(3).toString('hex').toUpperCase();  // 6 characters
}

const Room = mongoose.model('Room', roomSchema);

// CRUD APIs for Rooms
app.post('/rooms', async (req, res) => {
    try {
        var room = new Room(req.body);
        await room.save()
        // .then(room => room.populate('created_by').populate('members').populate('queue').populate('current_song'));
        room = await Room.findById(room._id).populate('created_by').populate('members').populate('queue').populate('current_song');
        res.status(201).json(room);
        console.log("Room created:", room);
    } catch (err) {
        res.status(400).json({ error: err.message });
        console.error("Error creating room:", err.message);
    }
});

app.get('/rooms/', async (req, res) => {
    let filter = {};

    if (req.query.name) {
        filter.name = new RegExp(req.query.name, 'i'); 
    }

    if (req.query.code) {
        filter.code = req.query.code;
    }
    
    if (req.query.user_id) {
        filter.created_by = new mongoose.Types.ObjectId(req.query.user_id);
    }
    
    const room = req.query.code 
    ? await Room.findOne(filter).populate('created_by').populate('members').populate('queue').populate('current_song')
    : await Room.find(filter).populate('created_by').populate('members').populate('queue').populate('current_song');
    console.log("Rooms found:", room);
    room ? res.json(room) : res.status(404).json({ error: 'Room not found' });
});

app.get('/rooms/:id', async (req, res) => {
    const room = await Room.findById(req.params.id).populate('created_by').populate('members').populate('queue').populate('current_song');
    room ? res.json(room) : res.status(404).json({ error: 'Room not found' });
});

app.delete('/rooms/:id', async (req, res) => {
    await Room.findByIdAndDelete(req.params.id);
    res.json({ message: 'Room deleted' });
});

app.delete('/rooms/:id', async (req, res) => {
    await Room.findByIdAndDelete(req.params.id);
    res.json({ message: 'Room deleted' });
});

app.put('/rooms/:id', async (req, res) => {
    try {
        const room = await Room.findByIdAndUpdate(req.params.id, req.body, { new: true });
        res.json(room);
    } catch (err) {
        res.status(400).json({ error: err.message });
    }
});
        
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
        console.log(filters);
        res.json(songs);
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// Search Song by title
app.get('/api/songs', async (req, res) => {
    const { title } = req.query;
    let filter = {};
    if (title) {
        filter.title = { $regex: `.*${title}.*`, $options: 'i' };
    }

    const songs = await Song.find(filter).populate('uploaded_by');
    res.json(songs);
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
// app.post('/likes', async (req, res) => {
//     try {
//         const like = new Like(req.body);
//         await like.save();
//         const populatedLike = await Like.findById(like._id)
//             .populate('user_id')
//             .populate({
//                 path: 'post_id',
//                 populate: [
//                     { path: 'user_id' },
//                     { path: 'song_id', populate: { path: 'uploaded_by' } }
//                 ]
//             });
//         res.status(201).json(populatedLike);
//     } catch (err) {
//         res.status(400).json({ error: err.message });
//     }
// });

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
// app.delete('/likes/:id', async (req, res) => {
//     try {
//         const like = await Like.findByIdAndDelete(req.params.id);
//         if (!like) return res.status(404).json({ error: 'Like not found' });
//         res.json({ message: 'Like deleted' });
//     } catch (err) {
//         res.status(500).json({ error: err.message });
//     }
// });

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