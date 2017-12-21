package me.shrp.blog.OKHttpTest;

import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Description
 * Created by lynxs on 2017/12/19.
 */
public class Test {

    public static final long MAX_SIZE = 512L * 1024 * 1024;

    public static void main(String[] args) throws IOException {
        simpleGet(new OkHttpClient());
        simplePost(new OkHttpClient());
        multipartPost(new OkHttpClient());
        cachedGet();
        asyncGet(new OkHttpClient());
        interceptGet();
    }

    private static void interceptGet() throws IOException {
        final File cacheDir = Files.createTempDirectory("test").toFile();
        final Cache cache = new Cache(cacheDir, MAX_SIZE);
        final OkHttpClient okHttpClient = new OkHttpClient.Builder().cache(cache)
                .addInterceptor(chain -> {
//                    System.out.println("intercept 1.");
                    final Response proceed = chain.proceed(chain.request());
                    System.out.println("intercept 1 return.");
                    return proceed;
                })
                .addInterceptor(chain -> {
                    System.out.println("intercept 2.");
                    final Response proceed = chain.proceed(chain.request());
                    System.out.println("intercept 2 return.");
                    return proceed;
                })
                .addNetworkInterceptor(chain -> {
                    System.out.println("network intercept.");
                    final Response proceed = chain.proceed(chain.request());
                    System.out.println("network intercept return.");
                    return proceed;
                })
                .build();
        final Request build = new Request.Builder().url("http://blog.shrp.me")
                .header("PostId", "12345")
                .addHeader("client-version", "3.9.1")
                .build();
        System.out.print("intercept get: ");
        executeRequest(okHttpClient, build);
        System.out.print("intercept get from cache: ");
        executeRequest(okHttpClient, build);
    }

    private static void simpleGet(final OkHttpClient okHttpClient) {
        final Request build = new Request.Builder().url("http://blog.shrp.me")
                .header("PostId", "12345")
                .addHeader("client-version", "3.9.1")
                .build();
        System.out.print("simple get: ");
        executeRequest(okHttpClient, build);
    }

    private static void cachedGet() throws IOException {
        final File cacheDir = Files.createTempDirectory("test").toFile();
        final Cache cache = new Cache(cacheDir, MAX_SIZE);
        final OkHttpClient okHttpClient = new OkHttpClient.Builder().cache(cache).build();
        final Request build = new Request.Builder().url("http://blog.shrp.me")
                .header("PostId", "12345")
                .cacheControl(CacheControl.FORCE_NETWORK)
                .addHeader("client-version", "3.9.1")
                .build();
        System.out.print("cached get: ");
        final Response response = executeRequest(okHttpClient, build);
        assert response.networkResponse() != null;
        final Request request2 = new Request.Builder().url("http://blog.shrp.me")
                .header("PostId", "12345")
                .cacheControl(CacheControl.FORCE_CACHE)
                .addHeader("client-version", "3.9.1")
                .build();
        final Response response2 = executeRequest(okHttpClient, request2);
        assert response2.cacheResponse() != null;
    }

    private static void asyncGet(final OkHttpClient okHttpClient) {
        final Request build = new Request.Builder().url("http://blog.shrp.me")
                .header("PostId", "12345")
                .addHeader("client-version", "3.9.1")
                .build();
        System.out.print("async get: ");
        okHttpClient.newCall(build).enqueue(new Callback() {
            @Override
            public void onFailure(final Call call, final IOException e) {
                System.out.println(call.request().url() + " is failed. " + e.getMessage());
            }

            @Override
            public void onResponse(final Call call, final Response response) throws IOException {
                if (response.isSuccessful()) {
                    System.out.println(response.code() + " : " + response.body().string().length());
                } else {
                    System.out.println(response.code());
                }
            }
        });
    }

    private static void simplePost(final OkHttpClient okHttpClient) {
        final MediaType parse = MediaType.parse("application/json");
        final RequestBody requestBody = RequestBody.create(parse, "{}");
        final Request build = new Request.Builder().url("http://blog.shrp.me")
                .post(requestBody)
                .header("PostId", "12345")
                .addHeader("client-version", "3.9.1")
                .build();
        System.out.print("simple post: ");
        executeRequest(okHttpClient, build);
    }

    private static void multipartPost(final OkHttpClient okHttpClient) {
        final MultipartBody form = new MultipartBody.Builder().addFormDataPart(
                "test", "test"
        ).build();
        final Request build = new Request.Builder().url("http://blog.shrp.me")
                .post(form)
                .header("PostId", "12345")
                .addHeader("client-version", "3.9.1")
                .build();
        System.out.print("multipart post: ");
        executeRequest(okHttpClient, build);
    }

    private static Response executeRequest(final OkHttpClient okHttpClient, final Request build) {
        try {
            final Response response = okHttpClient.newCall(build).execute();
            if (response.isSuccessful()) {
                System.out.println(response.code() + " : " + response.body().string().length());
            } else {
                System.out.println(response.code());
            }
            return response;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
