function fnApiGet(url, isAsync, headers, data, dataType, call_func) {
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
            fnErrorCheck(dataType, error)
        }
    });
}

function fnApiPost(url, isAsync, headers, data, dataType, call_func) {
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
            fnErrorCheck(dataType, error)
        },
        complete: function(res) {

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
    if (res.header.resultCode === 200) {
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

//쿠키 저장하는 함수
function setCookie(name, value, unixTime) {
    var date = new Date();
    date.setTime(date.getTime() + unixTime);
    document.cookie = encodeURIComponent(name) + '=' + encodeURIComponent(value) + ';expires=' + date.toUTCString() + ';path=/';
}

//쿠키 값 가져오는 함수
function getCookie(name) {
    var value = document.cookie.match('(^|;) ?' + name + '=([^;]*)(;|$)');
    return value? value[2] : null;
}

//쿠키 삭제하는 함수
function deleteCookie(name) {
    document.cookie = encodeURIComponent(name) + '=; expires=Thu, 01 JAN 1999 00:00:10 GMT';
}
