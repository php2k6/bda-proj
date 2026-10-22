// Optional MongoDB exercise. The local web app does not need MongoDB.
// mongoimport --db moviematch --collection recommendations --drop --file artifacts/recommendations.ndjson
// --drop replaces this collection; omit it when preserving existing documents.
use('moviematch');
db.recommendations.createIndex({userId: 1}, {unique: true});
db.recommendations.findOne({userId: 1});
