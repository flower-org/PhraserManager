package com.phraser.db;

import static com.phraser.schema.phraser.Icon.*;

public enum Icon {
    KEY(Key),
    LOGIN(Login),
    ASTERISK(Asterisk),
    LOCK(Lock),
    AA(Aa),
    STAR(Star),

    SETTINGS(Settings),
    FOLDER(Folder),
    TO_PARENT_FOLDER(ToParentFolder),
    LOOKING_GLASS(LookingGlass),
    LT_TRIANGLE(LTTriangle),
    GT_TRIANGLE(GTTriangle),

    TEXT_OUT(TextOut),
    LEDGER(Ledger),
    PLUS_MINUS(PlusMinus),
    STARS(Stars),
    MESSAGE(Message),
    QUOTE(Quote),

    QUESTION(Question),
    PLUS(Plus),
    MINUS(Minus),
    X(com.phraser.schema.phraser.Icon.X),
    CHECK(Check),
    COPY(Copy),
    DOWNLOAD(Download),
    UPLOAD(Upload);

    public final byte code;

    Icon(byte code) {
        this.code = code;
    }

    public static Icon fromCode(byte code) {
        switch (code) {
            case Key : return KEY;
            case Login : return LOGIN;
            case Asterisk : return ASTERISK;
            case Lock : return LOCK;
            case Aa : return AA;
            case Star : return STAR;

            case Settings : return SETTINGS;
            case Folder : return FOLDER;
            case ToParentFolder : return TO_PARENT_FOLDER;
            case LookingGlass : return LOOKING_GLASS;
            case LTTriangle : return LT_TRIANGLE;
            case GTTriangle : return GT_TRIANGLE;

            case TextOut : return TEXT_OUT;
            case Ledger : return LEDGER;
            case PlusMinus : return PLUS_MINUS;
            case Stars : return STARS;
            case Message : return MESSAGE;
            case Quote : return QUOTE;

            case Question : return QUESTION;
            case Plus : return PLUS;
            case Minus : return MINUS;
            case com.phraser.schema.phraser.Icon.X : return X;
            case Check : return CHECK;
            case Copy : return COPY;
            case Download : return DOWNLOAD;
            case Upload : return UPLOAD;

            default: throw new RuntimeException("Unknown Icon code " + code);
        }
    }
}
