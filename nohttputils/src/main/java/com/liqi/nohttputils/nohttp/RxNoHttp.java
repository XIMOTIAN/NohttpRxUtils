package com.liqi.nohttputils.nohttp;

import android.app.Dialog;
import android.content.Context;
import android.widget.Toast;

import com.liqi.nohttputils.R;
import com.liqi.nohttputils.interfa.DialogGetInterfa;
import com.liqi.nohttputils.interfa.RequestOkAndNo;
import com.yolanda.nohttp.NoHttp;
import com.yolanda.nohttp.error.NetworkError;
import com.yolanda.nohttp.error.NotFoundCacheError;
import com.yolanda.nohttp.error.ParseError;
import com.yolanda.nohttp.error.TimeoutError;
import com.yolanda.nohttp.error.URLError;
import com.yolanda.nohttp.error.UnKnownHostError;
import com.yolanda.nohttp.rest.IParserRequest;
import com.yolanda.nohttp.rest.Response;

import java.net.ProtocolException;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


/**
 * 用rxjava去请求nohttp
 * Created by  Liqi on 2016/12/08.
 */
class RxNoHttp {
    private static RxNoHttp rxNoHttp;
    private Dialog dialog = null;

    private RxNoHttp() {

    }

    synchronized static RxNoHttp getRxNoHttp() {
        return rxNoHttp = (null == rxNoHttp ? new RxNoHttp() : rxNoHttp);
    }

    /**
     * 通过nohttp去请求
     *
     * @param mDialogGetInterfa dialog获取接口
     * @param responseInterfa   请求成功或者失败回调对象
     */
    <T> void request(final IParserRequest<T> request, DialogGetInterfa mDialogGetInterfa, final RequestOkAndNo<T> responseInterfa) {
        if (null != mDialogGetInterfa)
            dialog = mDialogGetInterfa.getDialog();

        if (null != dialog)
            dialog.show();

        Observable.create(new Observable.OnSubscribe<Response<T>>() {
            @Override
            public void call(Subscriber<? super Response<T>> subscriberOut) {
                // 最关键的就是用NoHttp的同步请求请求到response了，其它的都是rxjava做的，跟nohttp无关的。
                Response<T> response = NoHttp.startRequestSync(request);
                if (response.isSucceed())
                    subscriberOut.onNext(response);
                else
                    subscriberOut.onError(response.getException());
                subscriberOut.onCompleted();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Response<T>>() {
                    @Override
                    public void onCompleted() {
                        if (null != dialog) {
                            if (dialog.isShowing()) {
                                dialog.dismiss();
                            }
                            dialog = null;
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        // 关闭dialog.
                        if (null != dialog) {
                            if (dialog.isShowing()) {
                                dialog.dismiss();
                            }
                            // 提示异常信息。
                            if (e instanceof NetworkError) {// 网络不好
                                show(R.string.error_please_check_network);
                            } else if (e instanceof TimeoutError) {// 请求超时
                                show(R.string.error_timeout);
                            } else if (e instanceof UnKnownHostError) {// 找不到服务器
                                show(R.string.error_not_found_server);
                            } else if (e instanceof URLError) {// URL是错的
                                show(R.string.error_url_error);
                            } else if (e instanceof NotFoundCacheError) {
                                // 这个异常只会在仅仅查找缓存时没有找到缓存时返回
                                show(R.string.error_not_found_cache);
                            } else if (e instanceof ProtocolException) {
                                show(R.string.error_system_unsupport_method);
                            } else if (e instanceof ParseError) {
                                show(R.string.error_parse_data_error);
                            } else {
                                show(R.string.error_unknow);
                            }
                            dialog = null;
                        }
                        if (null != responseInterfa)
                            responseInterfa.onError(e);
                    }

                    @Override
                    public void onNext(Response<T> tResponse) {
                        if (null != responseInterfa)
                            responseInterfa.onNext(tResponse.get());
                    }
                });
    }

    /**
     * 土司提示
     *
     * @param stringId 提示内容资源ID
     */
    private void show(int stringId) {
        Context context = dialog.getContext();
        Toast.makeText(context, context.getResources().getString(stringId), Toast.LENGTH_SHORT).show();
    }
}
