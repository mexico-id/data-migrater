package io.mosip.data.constant;

public enum ImageFormat {
    JPEG("jpg", "jpg"),
    JP2("jpeg 2000", "jp2"),
    WSQ("wsq","wsq");

    public final String format;
    public final String fileFormat;

    ImageFormat(final String format, final String fileFormat) {
        this.format = format;
        this.fileFormat = fileFormat;
    }

    public String getFormat() {
        return format;
    }

    public String getFileFormat() {
        return fileFormat;
    }
}