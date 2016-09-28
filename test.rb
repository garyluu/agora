#!/usr/bin/ruby -w

require "json"
require "open3"

$github_token = ENV["GITHUB_TOKEN"]
if $github_token.nil?
  begin
    $github_token = File.read("#{ENV['HOME']}/.github-token").chomp
  rescue StandardError
    nil
  end
end
if $github_token.nil?
  STDERR.puts "Could not find Github token. Tried GITHUB_TOKEN environment variable and " +
                  "#{ENV['HOME']}/.github-token"
  exit 1
end

$app_name = ENV.fetch("PROJECT") {|_|
  STDERR.puts "PROJECET not set."
  exit 1
}
$branch = ENV.fetch("BRANCH") {|_|
  STDERR.puts "BRANCH not set."
  exit 1
}
$commit_sha = ENV.fetch("GIT_SHA") {|_|
  STDERR.puts "GIT_SHA not set."
  exit 1
}

github_api_url_root = "https://api.github.com/repos/broadinstitute/firecloud-develop"

def call_github(url, *extra_curl_params)
  curl_cmd = [
      "curl", "-f",
      "-H", "Authorization: token #{$github_token}",
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

resp = call_github(github_api_url_root)

puts resp