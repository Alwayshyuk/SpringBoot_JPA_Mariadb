package org.zerock.mreview.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMovieImage is a Querydsl query type for MovieImage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMovieImage extends EntityPathBase<MovieImage> {

    private static final long serialVersionUID = 237425113L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMovieImage movieImage = new QMovieImage("movieImage");

    public final StringPath imgName = createString("imgName");

    public final NumberPath<Long> inum = createNumber("inum", Long.class);

    public final QMovie movie;

    public final StringPath path = createString("path");

    public final StringPath uuid = createString("uuid");

    public QMovieImage(String variable) {
        this(MovieImage.class, forVariable(variable), INITS);
    }

    public QMovieImage(Path<? extends MovieImage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMovieImage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMovieImage(PathMetadata metadata, PathInits inits) {
        this(MovieImage.class, metadata, inits);
    }

    public QMovieImage(Class<? extends MovieImage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.movie = inits.isInitialized("movie") ? new QMovie(forProperty("movie")) : null;
    }

}

