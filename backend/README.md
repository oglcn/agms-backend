# AGMS Backend

A Spring Boot backend application with PostgreSQL database and JWT authentication.

## 🛠 Tech Stack

- **Backend**: Spring Boot + PostgreSQL
- **Authentication**: JWT (JSON Web Tokens)
- **Database**: PostgreSQL
- **Deployment**: Railway
- **Frontend Integration**: Next.js (deployed on Vercel)

## 📋 Prerequisites

- Java 17 or higher
- Maven
- PostgreSQL
- Railway account
- GitHub account

## 🚀 Getting Started

### Local Development Setup

1. Clone the repository:

```bash
git clone https://github.com/your-username/agms-backend.git
cd agms-backend
```

2. Configure PostgreSQL:

   - Install PostgreSQL if not already installed
   - Create a new database named `agms`
   - Create `.env.local` for `application.properties`

3. Create .env.local:

```properties
DB_URL=jdbc:postgresql://localhost:5432/agms
DB_USERNAME=userName
DB_PASSWORD=password

EMAIL_PASSWORD=password
EMAIL_ADDRESS=...@std.iyte.edu.tr
```

4. Run the application:

```bash
mvn spring-boot:run
```

## 🔐 JWT Authentication

### Backend Implementation

1. JWT Configuration:

   - Secret key is stored in `application.properties`
   - Token expiration time is configurable
   - Claims include user ID and roles

2. Authentication Endpoints:

   - POST `/api/auth/register` - User registration
   - POST `/api/auth/login` - User login
   - POST `/api/auth/logout` - User logout

3. Protected Routes:
   - All routes under `/api/**` (except auth endpoints) require JWT
   - Token must be sent in Authorization header: `Bearer <token>`

### Frontend Integration

1. API Calls:

```javascript
// Example API call with JWT
const fetchData = async () => {
  const token = localStorage.getItem("token");
  const response = await fetch("${NEXT_PUBLIC_API_URL}/api/protected-route", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
  // Handle response
};
```

2. Route Protection:

```javascript
// Example route guard
useEffect(() => {
  const token = localStorage.getItem("token");
  if (!token) {
    router.push("/login");
  }
}, []);
```

## 🚂 Railway Deployment

1. Prepare for Deployment:

   - Ensure all environment variables are externalized
   - Update `application.properties` to use Railway's DATABASE_URL
   - Add `Procfile` for Railway deployment

2. Deploy to Railway:

   - Connect your GitHub repository to Railway
   - Add PostgreSQL plugin in Railway dashboard
   - Configure environment variables:
     - `DATABASE_URL` (automatically set by Railway)
     - `JWT_SECRET`
     - Other environment-specific variables

3. Update Frontend:
   - Set `NEXT_PUBLIC_API_URL` in Vercel to your Railway API URL
   - Example: `https://your-app-name.up.railway.app`

## 📦 Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── agms/
│   │           ├── config/
│   │           ├── controller/
│   │           ├── model/
│   │           ├── repository/
│   │           ├── service/
│   │           └── security/
│   └── resources/
│       └── application.properties
```

## 🔒 Security Best Practices

1. JWT Implementation:

   - Use secure, randomly generated secret keys
   - Implement token expiration
   - Include only necessary claims
   - Validate tokens on every request

2. API Security:
   - Enable CORS with specific origins
   - Implement rate limiting
   - Use HTTPS only
   - Sanitize user inputs
   - Implement proper error handling

## 🧪 Testing

1. Unit Tests:

```bash
mvn test
```

2. API Testing:
   - Use Postman or similar tools
   - Test all endpoints with and without authentication
   - Verify error handling

## 📝 API Documentation

API documentation is available at `/swagger-ui.html` when running the application.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
