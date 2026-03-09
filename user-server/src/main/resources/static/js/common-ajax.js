function fnApiGet(url, isAsync, headers, data, dataType, call_func, error_func, complete_func) {
    $.ajax({
        url : url,
        async : isAsync,
        type : 'get',
        headers : headers ? headers : {},
        data : data ? data : {},
        dataType : dataType, // json or html
        xhrFields: { withCredentials: true },
        success : function(res) {
            fnResCheck(dataType, res, call_func)
        },
        error: function(error) {
            if (error_func) {
                error_func(error);
            } else {
                fnErrorCheck(dataType, error);
            }
        },
        complete: function(res) {
            if (complete_func) complete_func(res);
        }
    });
}

function fnApiPost(url, isAsync, headers, data, dataType, call_func, error_func, complete_func) {
    $.ajax({
        url : url,
        async : isAsync,
        type : 'post',
        headers : headers ? headers : {},
        contentType: 'application/json',
        data : data ? JSON.stringify(data) : {},
        dataType : dataType, // json or html
        xhrFields: { withCredentials: true },
        success : function(res) {
            //call_func(res);
            fnResCheck(dataType, res, call_func)
        },
        error: function(error) {
            if (error_func) {
                error_func(error);
            } else {
                fnErrorCheck(dataType, error);
            }
        },
        complete: function(res) {
            if (complete_func) complete_func(res);
        }
    });
}

function fnApiPut(url, isAsync, headers, data, dataType, call_func, error_func, complete_func) {
    $.ajax({
        url : url,
        async : isAsync,
        type : 'put',
        headers : headers ? headers : {},
        contentType: 'application/json',
        data : data ? JSON.stringify(data) : {},
        dataType : dataType, // json or html
        xhrFields: { withCredentials: true },
        success : function(res) {
            fnResCheck(dataType, res, call_func)
        },
        error: function(error) {
            if (error_func) {
                error_func(error);
            } else {
                fnErrorCheck(dataType, error);
            }
        },
        complete: function(res) {
            if (complete_func) complete_func(res);
        }
    });
}

function fnApiPatch(url, isAsync, headers, data, dataType, call_func, error_func, complete_func) {
    $.ajax({
        url : url,
        async : isAsync,
        type : 'patch',
        headers : headers ? headers : {},
        contentType: 'application/json',
        data : data ? JSON.stringify(data) : {},
        dataType : dataType, // json or html
        xhrFields: { withCredentials: true },
        success : function(res) {
            fnResCheck(dataType, res, call_func)
        },
        error: function(error) {
            if (error_func) {
                error_func(error);
            } else {
                fnErrorCheck(dataType, error);
            }
        },
        complete: function(res) {
            if (complete_func) complete_func(res);
        }
    });
}

function fnApiDelete(url, isAsync, headers, data, dataType, call_func, error_func, complete_func) {
    $.ajax({
        url : url,
        async : isAsync,
        type : 'delete',
        headers : headers ? headers : {},
        contentType: 'application/json',
        data : data ? JSON.stringify(data) : {},
        dataType : dataType, // json or html
        xhrFields: { withCredentials: true },
        success : function(res) {
            fnResCheck(dataType, res, call_func)
        },
        error: function(error) {
            if (error_func) {
                error_func(error);
            } else {
                fnErrorCheck(dataType, error);
            }
        },
        complete: function(res) {
            if (complete_func) complete_func(res);
        }
    });
}

function fnResCheck(dataType, res, callback_func) {
    // 응답값이 html일 경우
    if(dataType === 'html'){
        const parser = new DOMParser();
        const doc = parser.parseFromString(res, 'text/html');
        const title = doc.title;

        // title이 "ERROR"인지 확인
        if (title === "ERROR") {
            // HTML의 모든 <script> 태그 가져오기
            const error = doc.querySelector("h1");
            alert(error);
            //            const newScript = document.createElement("script");
            //            newScript.textContent = errorScript.textContent;
            //            document.body.appendChild(newScript);
        } else {
            callback_func(res);//그리기
        }
        return;
    }

    // 응답값이 json일 경우
    if (res) {
        callback_func(res);
    } else {
        // const msgData = res.header.resultMessage ? res.header.resultMessage : '영어로 바꾸시오. 유효하지 않은 호출입니다. '
        // alert(msgData);
        alert("유효하지 않은 호출입니다.")
    }
}

function fnErrorCheck(dataType, error) {
    alert("에러 발생")
}
