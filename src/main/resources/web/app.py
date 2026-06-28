import os
import requests
from flask import Flask, render_template, request, jsonify

app = Flask(__name__)

JAVA_API = os.environ.get("JAVA_API_URL", "http://localhost:12036") + "/api/v1"


@app.route('/')
def index():
    return render_template('index.html')


@app.route('/api/status')
def status():
    try:
        r = requests.get(f"{JAVA_API}/transport/routes/status", timeout=5)
        return jsonify(r.json())
    except Exception:
        return jsonify({"tflReady": False, "mergedReady": False, "ready": False, "error": True})


@app.route('/api/refresh', methods=['POST'])
def refresh():
    try:
        r = requests.get(f"{JAVA_API}/transport/routes", timeout=120)
        return r.text, r.status_code
    except Exception as e:
        return str(e), 500


@app.route('/api/stations')
def get_stations():
    try:
        r = requests.get(f"{JAVA_API}/transport/stations", timeout=10)
        return jsonify(r.json())
    except Exception as e:
        return str(e), 500


@app.route('/api/guess', methods=['POST'])
def add_guess():
    try:
        r = requests.post(f"{JAVA_API}/transport/guess",
                          json=request.json,
                          timeout=10)
        return r.text, r.status_code
    except Exception as e:
        return str(e), 500


@app.route('/api/guesses')
def get_guesses():
    try:
        r = requests.get(f"{JAVA_API}/transport/guess", timeout=10)
        return jsonify(r.json())
    except Exception as e:
        return str(e), 500


@app.route('/api/guesses', methods=['DELETE'])
def reset_guesses():
    try:
        r = requests.delete(f"{JAVA_API}/transport/guess", timeout=10)
        return r.text, r.status_code
    except Exception as e:
        return str(e), 500


@app.route('/api/results')
def get_results():
    try:
        r = requests.get(f"{JAVA_API}/transport/guess/results", timeout=10)
        return jsonify(r.json())
    except Exception as e:
        return str(e), 500


@app.route('/api/breakdown')
def get_breakdown():
    try:
        r = requests.get(f"{JAVA_API}/transport/guess/breakdown", timeout=10)
        return jsonify(r.json())
    except Exception as e:
        return str(e), 500


if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=8080)
