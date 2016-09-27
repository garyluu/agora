#!/usr/bin/ruby -w

require "json"
require "open3"

github_api_url_root = "https://api.github.com/repos/broadinstitute/firecloud-develop"

$branch = ENV.fetch("BRANCH") {|_|
  STDERR.puts "BRANCH not set."
  exit 1
}

def call_github(url, *extra_curl_params)
  curl_cmd = [
      "curl", "-f",
      #"-H", "Authorization: token #{$github_token}",
      *extra_curl_params,
      "#{url}"
  ]
  Open3.popen3(*curl_cmd) {|stdin, stdout, stderr, wait_thread|
    if wait_thread.value.success?
      yield JSON.load(stdout)
    else
      STDERR.puts "curl call failed:"
      STDERR.puts curl_cmd.inspect
      STDERR.puts stderr.read
      exit 1
    end
  }
end

url = "#{github_api_url_root}/git/refs/heads/develop"
latest_commit_url, latest_commit_sha = call_github(url) {|response|
  o = response["object"]
  [o["url"], o["sha"]]
}

puts latest_commit_url